package com.styxxco.cliquer.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.styxxco.cliquer.domain.Account;
import com.styxxco.cliquer.domain.Group;
import com.styxxco.cliquer.domain.Skill;
import com.styxxco.cliquer.domain.Message;
import com.styxxco.cliquer.domain.Message.Types;
import com.styxxco.cliquer.security.FirebaseTokenHolder;
import com.styxxco.cliquer.service.GroupService;
import lombok.extern.log4j.Log4j;
import org.bson.types.ObjectId;
import com.styxxco.cliquer.database.*;
import com.styxxco.cliquer.domain.*;
import com.styxxco.cliquer.security.SecurityConfiguration;
import com.styxxco.cliquer.service.AccountService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.styxxco.cliquer.domain.Message.Types.FRIEND_INVITE;

@Log4j
@Service(value = AccountServiceImpl.NAME)
public class AccountServiceImpl implements AccountService {
    public final static String NAME = "AccountService";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupService groupService;

    public AccountServiceImpl() {

    }

    public AccountServiceImpl(AccountRepository ar, SkillRepository sr, MessageRepository mr, GroupRepository gr)
    {
        this.accountRepository = ar;
        this.skillRepository = sr;
        this.messageRepository = mr;
        this.groupRepository = gr;
        this.groupService = new GroupServiceImpl(ar, sr, mr, gr);
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails userDetails = accountRepository.findByUsername(username);
        if (userDetails == null)
            return null;

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        for (GrantedAuthority role: userDetails.getAuthorities()) {
            grantedAuthorities.add(new SimpleGrantedAuthority(role.getAuthority()));
        }

        return new User(userDetails.getUsername(), userDetails.getPassword(), userDetails.getAuthorities());
    }

    @Override
    public Account createAccount(String username, String email, String firstName, String lastName)
    {
        if(accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " already exists");
            return null;
        }
        Account user = new Account(username, email, firstName, lastName);
        this.accountRepository.save(user);
        return user;
    }

    @Override
    public Account deleteAccount(String username)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        for(ObjectId groupID : user.getGroupIDs())
        {
            Group group = groupRepository.findByGroupID(groupID);
            group.removeGroupMember(user.getAccountID());
            if(group.getGroupLeaderID().equals(user.getAccountID()))
            {
                group.setGroupLeaderID(group.getGroupMemberIDs().get(0));
            }
            groupRepository.save(group);
        }
        accountRepository.delete(user);
        return user;
    }

    @Override
    @Transactional
    public Account registerUser(FirebaseTokenHolder tokenHolder, String firstName, String lastName) {

        Account userLoaded = accountRepository.findByUsername(tokenHolder.getUid());

        if (userLoaded == null) {
            Account account = createAccount(tokenHolder.getUid(), tokenHolder.getEmail(), firstName, lastName);
            account.setAuthorities(getUserRoles());
            account.setPassword(UUID.randomUUID().toString());
            System.out.println(account.toString());
            accountRepository.save(account);
            log.info("registerUser -> user \"" + account.getFirstName() + " " + account.getLastName() + "\" created");
            return account;
        } else {
            log.info("registerUser -> user \"" + tokenHolder.getUid() + "\" exists");
            return userLoaded;
        }
    }

    @Override
    public List<Role> getModRoles() {
        return Collections.singletonList(getRole(SecurityConfiguration.Roles.ROLE_MOD));
    }

    @Override
    public List<Role> getUserRoles() {
        return Collections.singletonList(getRole(SecurityConfiguration.Roles.ROLE_USER));
    }

    @Override
    public List<Role> getAnonRoles() {
        return Collections.singletonList(getRole(SecurityConfiguration.Roles.ROLE_ANONYMOUS));
    }

    private Role getRole(String authority) {
        Role modRole = roleRepository.findByAuthority(authority);
        if (modRole == null) {
            return new Role(authority);
        } else {
            return modRole;
        }
    }

    @Override
    public Account getProfile(String username, String type) {
        Account user = null;
        switch (type) {
            case "user":
                user = getUserProfile(username);
                break;
            case "member":
                user = getMemberProfile(username);
                break;
            case "public":
                user = getPublicProfile(username);
                break;
        }
        return user;
    }

    @Override
    public Account getUserProfile(String username)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        user.setTimer();
        accountRepository.save(user);
        return user;
    }

    // TODO: add group privacy search
    @Override
    public Map<String, ? extends Searchable> searchWithFilter(String type, String query, int level, boolean suggestions, boolean weights) {
        switch(type) {
            case "firstName":
                return searchByFirstName(query).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "lastName":
                return searchByLastName(query).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "fullName":
                return searchByFullName(query).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "username":
                Map<String, Account> map = new HashMap<>();
                Account account = searchByUsername(query);
                map.put(account.getUsername(), account);
                return map;
            case "reputation":
                return searchByReputation(level, suggestions, weights).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "skill":
                return searchBySkill(query, level).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "groupName":
                return searchByGroupName(query).stream().collect(Collectors.toMap(Group::getGid, _it -> _it));
        }
        return null;
    }

    @Override
    public Map<String, ? extends Searchable> searchWithFilter(String type, String query, boolean suggestions, boolean weights) {
        System.out.println(type + " " + query);
        switch(type) {
            case "firstname":
                return searchByFirstName(query).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "lastname":
                return searchByLastName(query).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "fullname":
                return searchByFullName(query).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "username":
                Map<String, Account> map = new HashMap<>();
                Account account = searchByUsername(query);
                map.put(account.getUsername(), account);
                return map;
            case "reputation":
                return searchByReputation(Integer.parseInt(query), suggestions, weights).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "skill":
                return searchBySkill(query).stream().collect(Collectors.toMap(Account::getUsername, _it -> _it));
            case "groupname":
                return searchByGroupName(query).stream().collect(Collectors.toMap(Group::getGid, _it -> _it));
            case "ispublic":
                return searchByGroupPublic(Boolean.parseBoolean(query)).stream().collect(Collectors.toMap(Group::getGid, _it -> _it));
        }
        return null;
    }

    @Override
    public Account getMemberProfile(String username)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        /* Mask private information and settings */
        user.setModerator(false);
        user.setMessageIDs(null);
        user.setPassword(null);
        user.setOptedOut(false);
        user.setProximityReq(-1);
        user.setReputationReq(-1.0);
        return user;
    }
    
    @Override
    public Account maskPublicProfile(Account account)
    {
        if(!account.isPublic())
        {
            /* Mask all information except name and reputation */
            account.setSkillIDs(null);
            account.setGroupIDs(null);
            account.setFriendIDs(null);
        }
        /* Mask private information and settings */
        account.setModerator(false);
        account.setMessageIDs(null);
        account.setPassword(null);
        account.setOptedOut(false);
        account.setEmail(null);
        account.setLoggedInTime(-1);
        account.setProximityReq(-1);
        account.setReputationReq(-1.0);
        return account;
    }

    @Override
    public Account getPublicProfile(String username)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        return this.maskPublicProfile(user);
    }

    @Override
    public List<Account> searchByFirstName(String firstName)
    {
        List<Account> accounts = accountRepository.findAccountsByFirstNameIsLike(firstName);
        List<Account> masked = new ArrayList<>();
        for(Account account : accounts)
        {
            if(!account.isOptedOut())
            {
                masked.add(this.maskPublicProfile(account));
            }
        }
        Comparator<Account> byLastName = Comparator.comparing(Account::getLastName);
        masked.sort(byLastName);
        return masked;
    }

    @Override
    public List<Account> searchByLastName(String lastName)
    {
        List<Account> accounts = accountRepository.findByLastName(lastName);
        List<Account> masked = new ArrayList<>();
        for(Account account : accounts)
        {
            if(!account.isOptedOut())
            {
                masked.add(this.maskPublicProfile(account));
            }
        }
        Comparator<Account> byFirstName = Comparator.comparing(Account::getFirstName);
        masked.sort(byFirstName);
        return masked;
    }

    @Override
    public List<Account> searchByFullName(String firstName, String lastName)
    {
        List<Account> accounts = accountRepository.findByFirstName(firstName);
        List<Account> masked = new ArrayList<>();
        for(Account account : accounts)
        {
            if(account.getLastName().equals(lastName) && !account.isOptedOut())
            {
                masked.add(this.maskPublicProfile(account));
            }
        }
        return masked;
    }

    @Override
    public List<Account> searchByFullName(String fullName) {
        String arr[] = fullName.split(" ");
        if (arr.length == 2) {
            return searchByFullName(arr[0], arr[1]);
        }
        return null;
    }


    @Override
    public List<Account> searchByReputation(int minimumRep, boolean includeSuggested, boolean includeWeights)
    {
        List<Account> accounts = accountRepository.findAll();
        List<Account> qualified = new ArrayList<>();
        for(Account account : accounts)
        {
            int accountReputation;
            if(includeWeights)
            {
                accountReputation = account.getAdjustedReputation();
            }
            else
            {
                accountReputation = account.getReputation();
            }
            if(accountReputation >= minimumRep && !account.isOptedOut() &&
                    account.getReputation()*account.getReputationReq() <= minimumRep)
            {
                qualified.add(this.maskPublicProfile(account));
            }
        }
        Comparator<Account> byFirstName = Comparator.comparing(Account::getFirstName);
        qualified.sort(byFirstName);
        Comparator<Account> byLastName = Comparator.comparing(Account::getLastName);
        qualified.sort(byLastName);
        Comparator<Account> byReputation = Comparator.comparingInt(Account::getReputation);
        byReputation = byReputation.reversed();
        qualified.sort(byReputation);
        if(includeSuggested)
        {
            return this.moveSuggestedToTop(qualified, minimumRep, includeWeights);
        }
        return qualified;
    }

    @Override
    public List<Account> moveSuggestedToTop(List<Account> accounts, int reputation, boolean includeWeights)
    {
        if(accounts.size() < 5)
        {
            return accounts;
        }
        int suggestions = Math.min(5, accounts.size()/5);
        List<Account> suggested = new ArrayList<>();
        for(Account account : accounts)
        {
            int accountReputation;
            if(includeWeights)
            {
                accountReputation = account.getAdjustedReputation();
            }
            else
            {
                accountReputation = account.getReputation();
            }
            if(accountReputation >= reputation && accountReputation <= reputation + Account.MAX_REP/10)
            {
                suggested.add(account);
            }
        }
        List<Account> results = new ArrayList<>();
        for(int i = 0; i < suggestions && suggested.size() > 0; i++)
        {
            Account random = suggested.get((int)(Math.random()*suggested.size()));
            suggested.remove(random);
            accounts.remove(random);
            results.add(random);
        }
        Comparator<Account> byFirstName = Comparator.comparing(Account::getFirstName);
        results.sort(byFirstName);
        Comparator<Account> byLastName = Comparator.comparing(Account::getLastName);
        results.sort(byLastName);
        Comparator<Account> byReputation = Comparator.comparingInt(Account::getReputation);
        byReputation = byReputation.reversed();
        results.sort(byReputation);
        results.add(null);
        results.addAll(accounts);
        return results;
    }

    // TODO: sort by level for front end
    @Override
    public List<Account> searchBySkill(String skillName) {
        List<Account> accounts = accountRepository.findAll();
        Comparator<Account> byFirstName = Comparator.comparing(Account::getFirstName);
        accounts.sort(byFirstName);
        Comparator<Account> byLastName = Comparator.comparing(Account::getLastName);
        accounts.sort(byLastName);

        List<Account> qualified = new ArrayList<>();
        for (Account account : accounts) {
            Skill skill = this.getSkill(account.getUsername(), skillName);
            if (account.isPublic() && skill != null) {
                qualified.add(this.maskPublicProfile(account));
            }
        }
        return qualified;
    }

    @Override
    public Account searchByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    @Override
    public List<Group> searchByGroupName(String name) {
        return groupRepository.findAllByGroupName(name);
    }

    @Override
    public List<Group> searchByGroupPublic(boolean isPublic) {
        return groupRepository.findAllByPublic(isPublic);
    }

    @Override
    public List<Skill> addSkills(String username, String json) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        List<Skill> skills = new ArrayList<>();
        try {
            ObjectMapper om = new ObjectMapper();
            TypeFactory typeFactory = om.getTypeFactory();
            List<String> list = om.readValue(json, typeFactory.constructCollectionType(List.class, String.class));
            for (String s: list) {
                skills.add(addSkill(user, s, "0"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return skills;
    }

    @Override
    public Account addSkills(String username, String json) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);

        try {
            ObjectMapper om = new ObjectMapper();
            TypeReference<List<HashMap<String, String>>> typeRef = new TypeReference<List<HashMap<String, String>>>() {};
            List<Map<String, String>> mapList = om.readValue(json, typeRef);
            for (Map<String, String> s: mapList) {
                String skillName = s.get("skillName");
                String skillLevel = s.get("skillLevel");

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public Skill addSkillToDatabase(String skillName)
    {
        if(skillRepository.existsBySkillName(skillName))
        {
            log.info("Skill " + skillName + " is already in database");
            return null;
        }
        Skill skill = new Skill(skillName, 0);
        skillRepository.save(skill);
        return skill;
    }

    @Override
    public List<Skill> getAllValidSkills()
    {
        List<Skill> skills = skillRepository.findBySkillLevel(0);
        Collections.sort(skills);
        return skills;
    }

    @Override
    public List<Skill> getAllUserSkills(String username)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        List<Skill> skills = new ArrayList<>();
        for(ObjectId skillID : user.getSkillIDs())
        {
            Skill skill = skillRepository.findBySkillID(skillID);
            skills.add(skill);
        }
        return skills;
    }

    @Override
    public List<Group> getAllUserGroups(String username) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        List<Group> groups = new ArrayList<>();
        for (ObjectId groupID: user.getGroupIDs()) {
            Group group = groupRepository.findByGroupID(groupID);
            groups.add(group);
        }
        return groups;
    }

    @Override
    public Skill getSkill(String username, String skillName)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        List<Skill> skills = this.getAllUserSkills(username);
        for(Skill skill : skills)
        {
            if(skill.getSkillName().equals(skillName))
            {
                return skill;
            }
        }
        return null;
    }

    @Override
    public Skill addSkill(String username, String skillName, String skillString)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        return addSkill(user, skillName, skillString);
    }

    private Skill addSkill(Account account, String skillName, String skillString) {
        if(!skillRepository.existsBySkillName(skillName))
        {
            log.info("Skill " + skillName + " is invalid");
            return null;
        }
        int skillLevel = 0;
        try {
            skillLevel = Integer.parseInt(skillString);
        } catch (NumberFormatException e) {
            log.info("Could not parse " + skillString + " as an integer");
        }
        if(skillLevel < 0 || skillLevel > 10)
        {
            log.info("Skill level " + skillLevel + " is invalid");
            return null;
        }

        Skill skill;
        if (skillRepository.existsBySkillNameAndSkillLevel(skillName, skillLevel)) {
            skill = skillRepository.findBySkillNameAndSkillLevel(skillName, skillLevel);
        } else if (skillRepository.existsBySkillName(skillName)){
            skill = new Skill(skillName, skillLevel);
            skillRepository.save(skill);
        } else {
            return null;
        }
        Skill curr = this.getSkill(account.getUsername(), skillName);
        if(curr != null)
        {
            if (curr.getSkillLevel() == skillLevel) {
                log.info("User " + account.getUsername() + " already has skill " + skillName);
                return null;
            } else {
                account.removeSkill(curr.getSkillID());
            }
        }
        account.addSkill(skill.getSkillID());
        accountRepository.save(account);
        return skill;
    }

    @Override
    public Account removeSkill(String username, String skillName)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        Skill skill = this.getSkill(username, skillName);
        if(skill == null)
        {
            log.info("User " + username + " does not have skill " + skillName);
            return null;

        }
        user.removeSkill(skill.getSkillID());
        accountRepository.save(user);
        return user;
    }

    @Override
    public List<Message> getNewMessages(String username) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        List<Message> messages = new ArrayList<>();
        for(ObjectId id : user.getMessageIDs())
        {
            Message message = messageRepository.findByMessageID(id);
            if(!message.isRead())
            {
                messages.add(message);
                message.setRead(true);
                messageRepository.save(message);
            }
        }
        return messages;
    }

     // Deprecated by Paula's chatlog but logic may be needed
    /*
    @Override
    public List<Message> getGroupChatLog(String username, String groupId, int lower, int upper) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        ObjectId groupID = new ObjectId(groupId);
        if(!groupRepository.existsByGroupID(groupID))
        {
            log.info("Group " + groupId + " not found");
            return null;
        }
        Group group = groupRepository.findByGroupID(groupID);
        if (!group.getGroupMemberIDs().contains(user.getAccountID())) {
            log.info("User is not apart of this group");
            return null;
        }
        List<ObjectId> messageIds = group.getChatLogsFrom(lower, upper);
        List<Message> messages = new ArrayList<>();
        for (ObjectId id: messageIds) {
            messages.add(messageRepository.findByMessageID(id));
        }
        return messages;
    }
    */

    // TODO: @Reed insert function to notify receiver in real time once web sockets done
    @Override
    public Message sendMessage(String username, ObjectId receiverID, String content, int type)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        if(!accountRepository.existsByAccountID(receiverID))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account sender = accountRepository.findByUsername(username);
        Account receiver = accountRepository.findByAccountID(receiverID);
        Message message = new Message(sender.getAccountID(), content, type);
        messageRepository.save(message);
        receiver.addMessage(message.getMessageID());
        accountRepository.save(receiver);
        return message;
    }

    @Override
    public Group createGroup(String username, String json) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        Group group = null;
        try {
            JSONObject obj = new JSONObject(json);
            String name = obj.getString("groupName");
            String purpose = obj.getString("groupPurpose");
            group = groupService.createGroup(name, purpose, user.getAccountID());
            for (Object k: obj.keySet()) {
                String key = k.toString();
                if (key.contentEquals("skillsReq")) {
                    JSONArray skills = obj.getJSONArray("skillsReq");
                    List<ObjectId> list = new ArrayList<>();
                    for (int i = 0; i < skills.length(); i++) {
                        if (skillRepository.existsBySkillName(skills.getString(i))) {
                            list.add(skillRepository.findBySkillNameAndSkillLevel(skills.getString(i), 0).getSkillID());
                        }
                    }
                    group.setSkillReqs(list);
                    groupRepository.save(group);
                }
                groupService.updateGroupSettings(group.getGroupID(), group.getGroupLeaderID(), key, obj.get(key).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return group;
    }

    /* TODO: Ensure user fits requirements @Shawn @SprintTwo */
    @Override
    public Account addToGroup(String username, ObjectId groupID) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        if(!groupRepository.existsByGroupID(groupID))
        {
            log.info("Group " + groupID + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        Group group = groupRepository.findByGroupID(groupID);

        groupService.addGroupMember(group.getGroupID(), group.getGroupLeaderID(), user.getAccountID());

        return user;
    }

    @Override
    public Account leaveGroup(String username, ObjectId groupID)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        if(!groupRepository.existsByGroupID(groupID))
        {
            log.info("Group " + groupID + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        Group group = groupRepository.findByGroupID(groupID);
        if(!groupService.hasGroupMember(group, user.getAccountID()))
        {
            log.info("User " + username + " is not in group " + groupID);
            return null;
        }
        if(group.getGroupLeaderID().equals(user.getAccountID()))
        {
            if(group.getGroupMemberIDs().size() == 1)
            {
                user.removeGroup(groupID);
                groupRepository.delete(group);
                accountRepository.save(user);
                return user;
            }
            else
            {
                group.setGroupLeaderID(group.getGroupMemberIDs().get(0));
                group.setOwnerUID(accountRepository.findByAccountID(group.getGroupLeaderID()).getUsername());
            }

        }
        group.removeGroupMember(user.getAccountID());
        groupRepository.save(group);
        user.removeGroup(groupID);
        accountRepository.save(user);
        return user;

    }

    @Override
    public Account inviteToGroup(String username, String friend, ObjectId groupID) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        if(!accountRepository.existsByUsername(friend))
        {
            log.info("User " + friend + " not found");
            return null;
        }
        if(!groupRepository.existsByGroupID(groupID))
        {
            log.info("Group " + groupID + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        Account account = accountRepository.findByUsername(friend);
        sendMessage(user.getUsername(), account.getAccountID(), groupID.toString(), Types.GROUP_INVITE);
        return account;
    }

    // TODO: send message to kicked to notify
    @Override
    public Account kickMember(String username, String friend, ObjectId groupID) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        if(!accountRepository.existsByUsername(friend))
        {
            log.info("User " + friend + " not found");
            return null;
        }
        if(!groupRepository.existsByGroupID(groupID))
        {
            log.info("Group " + groupID + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        Account account = accountRepository.findByUsername(friend);
        Group group = groupService.removeGroupMember(groupID, user.getAccountID(), account.getAccountID());
        if (group.getGroupMemberIDs().contains(account.getAccountID())) {
            return null;
        }
        return account;
    }

    @Override
    public Group deleteGroup(String username, ObjectId groupID) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        return groupService.deleteGroup(groupID, user.getAccountID());
    }

    @Override
    public double getReputationRanking(String username)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return 0.0;
        }
        Account user = accountRepository.findByUsername(username);
        List<Account> accounts = accountRepository.findAll();
        ArrayList<Integer> reputations = new ArrayList<>();
        for(Account account : accounts)
        {
            reputations.add(account.getReputation());
        }
        Collections.sort(reputations);
        int rank = reputations.lastIndexOf(user.getReputation()) + 1;
        return (100.0*rank)/reputations.size();
    }

    @Override
    public Message sendFriendInvite(String username, ObjectId receiverID)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        if(user.hasFriend(receiverID))
        {
            log.info("User " + username + " is already friends with " + receiverID);
            return null;
        }
        return this.sendMessage(username, receiverID,
                user.getFullName() + "wants to add you as a friend! Do you accept the friend invite?",
                Types.FRIEND_INVITE);
    }

    @Override
    public Account acceptFriendInvite(String username, ObjectId inviteID)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        if(!user.hasMessage(inviteID))
        {
            log.info("User " + username + " did not receive message " + inviteID);
            return null;
        }
        Message invite = messageRepository.findByMessageID(inviteID);
        user.removeMessage(inviteID);
        messageRepository.delete(invite);
        accountRepository.save(user);
        return this.addFriend(username, invite.getSenderID());
    }

    @Override
    public String rejectFriendInvite(String username, ObjectId inviteID)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        if(!user.hasMessage(inviteID))
        {
            log.info("User " + username + " did not receive message " + inviteID);
            return null;
        }
        user.removeMessage(inviteID);
        messageRepository.delete(inviteID.toString());
        accountRepository.save(user);
        return "Success";
    }

    @Override
    public Account addFriend(String username, ObjectId friendID) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        if(!accountRepository.existsByAccountID(friendID))
        {
            log.info("User " + friendID + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        Account friend = accountRepository.findByAccountID(friendID);
        user.addFriend(friend.getAccountID());
        friend.addFriend(user.getAccountID());
        accountRepository.save(user);
        accountRepository.save(friend);
        return friend;
    }

    @Override
    public Account removeFriend(String username, ObjectId friendID) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        if(!accountRepository.existsByAccountID(friendID))
        {
            log.info("User " + friendID + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        Account friend = accountRepository.findByAccountID(friendID);
        user.removeFriend(friend.getAccountID());
        friend.removeFriend(user.getAccountID());
        accountRepository.save(user);
        accountRepository.save(friend);
        return friend;
    }

    @Override
    public String checkNewUserFlag(String username)
    {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        user.incrementTimer();
        accountRepository.save(user);
        if(user.isNewUser())
        {
            return "New User";
        }
        return "Experienced User";
    }

    @Override
    public Account setAccountSettings(String username, String json) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        try {
            ObjectMapper om = new ObjectMapper();
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
            Map<String, String> map = om.readValue(json, typeRef);
            for (String key: map.keySet()) {
                switch(key) {
                    case "isPublic":
                        Boolean isPublic = Boolean.parseBoolean(map.get("isPublic"));
                        user.setPublic(isPublic);
                        break;
                    case "isOptedOut":
                        Boolean isOptedOut = Boolean.parseBoolean(map.get("isOptedOut"));
                        user.setOptedOut(isOptedOut);
                        break;
                    case "reputationReq":
                        Double repReq = Double.parseDouble(map.get("reputationReq"));
                        user.setReputationReq(repReq / user.getReputation());
                        break;
                    case "proximityReq":
                        Integer proxReq = Integer.parseInt(map.get("proximityReq"));
                        user.setProximityReq(proxReq);
                        break;
                }
            }
            accountRepository.save(user);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public Group setGroupSettings(String username, ObjectId groupId, String json) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        if (!groupRepository.existsByGroupID(groupId)) {
            log.info("Group " + groupId.toString() + " not found");
            return null;
        }
        Group group = groupRepository.findByGroupID(groupId);
        if (!group.getGroupLeaderID().equals(user.getAccountID())) {
            log.info("User does not have right to change settings");
            return null;
        }
        try {
            JSONObject obj = new JSONObject(json);
            for (Object k: obj.keySet()) {
                String key = k.toString();
                if (key.contentEquals("skillsReq")) {
                    JSONArray skills = obj.getJSONArray("skillsReq");
                    List<ObjectId> list = new ArrayList<>();
                    for (int i = 0; i < skills.length(); i++) {
                        if (skillRepository.existsBySkillName(skills.getString(i))) {
                            list.add(skillRepository.findBySkillNameAndSkillLevel(skills.getString(i), 0).getSkillID());
                        }
                    }
                    group.setSkillReqs(list);
                    groupRepository.save(group);
                }
                groupService.updateGroupSettings(groupId, user.getAccountID(), key, obj.get(key).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return group;
    }

    @Override
    public Account rateUser(String username, String friend, String json) {
        if(!accountRepository.existsByUsername(username))
        {
            log.info("User " + username + " not found");
            return null;
        }
        Account user = accountRepository.findByUsername(username);
        if(!accountRepository.existsByUsername(friend)) {
            log.info("User " + friend + " not found");
            return null;
        }
        Account other = accountRepository.findByUsername(friend);

        try {
            JSONObject obj = new JSONObject(json);
            for (Object k: obj.keySet()) {
                String key = k.toString();
                if (key.contentEquals("skillsRank")) {
                    JSONArray skills = obj.getJSONArray("skillsRank");
                    List<String> currSkillsString = new ArrayList<>();
                    List<ObjectId> currSkillsIds = other.getSkillIDs();
                    for (ObjectId id: currSkillsIds) {
                        currSkillsString.add(skillRepository.findBySkillID(id).getSkillName());
                    }
                    for (int i = 0; i < skills.length(); i++) {
                        JSONObject skill = skills.getJSONObject(i);
                        String skillName = skill.getString("skillName");
                        int add = skill.getInt("skillLevel");
                        if (skillRepository.existsBySkillName(skillName)) {
                            if (currSkillsString.contains(skillName)) {
                                int change = getSkill(other.getUsername(), skillName).getSkillLevel() + add;
                                addSkill(other, skillName, change + "");
                            }
                        }
                    }
                } else if (key.contentEquals("reputation")) {
                    other.setReputation(other.getReputation() + obj.getInt("reputation"));
                }
                accountRepository.save(other);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return other;
    }
}

