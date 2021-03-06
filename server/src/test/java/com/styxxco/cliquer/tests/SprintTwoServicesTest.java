package com.styxxco.cliquer.tests;

import com.styxxco.cliquer.database.*;
import com.styxxco.cliquer.domain.*;
import com.styxxco.cliquer.domain.Message.Types;
import com.styxxco.cliquer.service.AccountService;
import com.styxxco.cliquer.service.GroupService;
import com.styxxco.cliquer.service.impl.GroupServiceImpl;
import com.styxxco.cliquer.service.impl.AccountServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
public class SprintTwoServicesTest {

    @Autowired
    public AccountRepository accountRepository;
    @Autowired
    public SkillRepository skillRepository;
    @Autowired
    public MessageRepository messageRepository;
    @Autowired
    public GroupRepository groupRepository;
    @Autowired
    public RoleRepository roleRepository;
    @Autowired
    public GroupService groupService;
    @Autowired
    public AccountService accountService;

    /* Back end Unit Test for User Story 10 */
    @Test
    public void testGroupSearchFilters() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");

        Group cliquer = groupService.createGroup(
                "Cliquer",
                "To create a web app that facilitates the teaming of people who may have never met before",
                jordan.getAccountID());
        Group hula = groupService.createGroup(
                "Hoops",
                "To play hula hoops",
                shawn.getAccountID());
        Group hoops = groupService.createGroup(
                "Hoops",
                "To play basketball",
                shawn.getAccountID());
        Group games = groupService.createGroup(
                "Hoops",
                "To play basketball games",
                shawn.getAccountID());
        Group styxx = groupService.createGroup(
                "Styxx",
                "To make an app",
                shawn.getAccountID());

        jordan.setReputation(50);
        jordan.setReputationReq(0.5);
        jordan.setLatitude(40.00);
        jordan.setLongitude(-80.00);
        jordan.setProximityReq(Integer.MAX_VALUE);
        shawn.setReputation(60);
        shawn.setLatitude(40.2);
        shawn.setLongitude(-80.4);
        shawn.setProximityReq(Integer.MAX_VALUE);
        cliquer.setReputationReq(1.0);
        hula.setReputationReq(0.25);
        hoops.setReputationReq(0.5);
        games.setReputationReq(0.5);
        styxx.setReputationReq(0.9);

        cliquer.setPublic(true);
        hula.setPublic(true);
        hoops.setPublic(true);
        games.setPublic(false);
        styxx.setPublic(true);

        accountRepository.save(jordan);
        accountRepository.save(shawn);
        groupRepository.save(cliquer);
        groupRepository.save(hula);
        groupRepository.save(hoops);
        groupRepository.save(games);
        groupRepository.save(styxx);

        Skill lifter = accountService.addSkillToDatabase("Lifter");
        groupService.addSkillReq(hula.getGroupID(), shawn.getAccountID(), "Lifter", 2);
        groupService.addSkillReq(hoops.getGroupID(), shawn.getAccountID(), "Lifter", 6);

        List<Group> groups = groupService.searchByGroupName("hOOps", null);
        assertEquals(3, groups.size());
        assertEquals("Hoops", groups.get(0).getGroupName());

        groups = groupService.searchByGroupName("Hoops", null);
        assertEquals(3, groups.size());
        assertEquals("Hoops", groups.get(0).getGroupName());

        List<String> reqs = new ArrayList<>();
        reqs.add("Lifter");
        groups = groupService.searchBySkillReqs(reqs, groups);
        assertEquals(2, groups.size());
        assertEquals(1, groups.get(0).getSkillReqs().size());

        groups = groupService.searchBySettings("reed226", groups);
        assertEquals(2, groups.size());
        assertEquals("Hoops", groups.get(0).getGroupName());

        groups = groupService.searchBySettings("reed226", null);
        assertEquals(2, groups.size());
        assertEquals("Hoops", groups.get(0).getGroupName());

        List<Group> first = groupService.searchByLeaderFirstName("Shawn", null);
        List<Group> second = groupService.searchByLeaderFirstName("sHaWn", null);
        List<Group> third = groupService.searchByLeaderLastName("Montgomery", null);
        List<Group> fourth = groupService.searchByLeaderLastName("monTgoMery", null);
        List<Group> fifth = groupService.searchByLeaderFullName("Shawn", "Montgomery", null);
        List<Group> sixth = groupService.searchByLeaderFullName("shawN", "montgOMery", null);
        assertEquals(first.size(), second.size());
        assertEquals(first.size(), third.size());
        assertEquals(first.size(), fourth.size());
        assertEquals(first.size(), fifth.size());
        assertEquals(first.size(), sixth.size());

        groups = groupService.searchBySettings("montgo38", null);
        assertEquals(1, groups.size());
        assertEquals("Cliquer", groups.get(0).getGroupName());

        assertEquals(25, jordan.distanceTo(shawn.getLatitude(), shawn.getLongitude()));
        jordan.setProximityReq(30);
        shawn.setProximityReq(20);

        accountRepository.save(jordan);
        accountRepository.save(shawn);

        groups = groupService.searchBySettings("reed226", null);
        assertEquals(2, groups.size());
        assertEquals("Hoops", groups.get(0).getGroupName());

        groups = groupService.searchBySettings("montgo38", null);
        assertEquals(0, groups.size());
    }

    /* Back end Unit Test for User Story 13 */
    @Test
    public void testFriendInvites() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account kevin = accountService.createAccount("knagar", "montgo38@pdue.edu", "Kevin", "Nagar");

        Message invite = accountService.sendFriendInvite(jordan.getAccountID(), shawn.getAccountID());
        assertEquals(jordan.getAccountID(), invite.getSenderID());
        shawn = accountRepository.findByUsername(shawn.getUsername());
        assertEquals(invite.getType(), (int)shawn.getMessageIDs().get(invite.getMessageID()));
        assertEquals(Types.FRIEND_INVITE, invite.getType());
        String first = invite.getMessageID();

        invite = accountService.acceptFriendInvite(shawn.getAccountID(), invite.getMessageID());
        assertEquals(shawn.getAccountID(), invite.getSenderID());
        shawn = accountRepository.findByUsername(shawn.getUsername());
        assertEquals(jordan.getFullName(), shawn.getFriendIDs().get(jordan.getAccountID()));
        jordan = accountRepository.findByUsername(jordan.getUsername());
        assertEquals(shawn.getFullName(), jordan.getFriendIDs().get(shawn.getAccountID()));
        assertEquals(0, shawn.getMessageIDs().size());

        invite = accountService.sendFriendInvite(jordan.getAccountID(), kevin.getAccountID());
        assertEquals(jordan.getAccountID(), invite.getSenderID());
        kevin = accountRepository.findByUsername(kevin.getUsername());
        assertEquals(invite.getType(), (int)kevin.getMessageIDs().get(invite.getMessageID()));
        assertEquals(Types.FRIEND_INVITE, invite.getType());
        String second = invite.getMessageID();

        invite = accountService.rejectInvite(kevin.getAccountID(), invite.getMessageID());
        assertEquals(jordan.getAccountID(), invite.getSenderID());
        kevin = accountRepository.findByUsername(kevin.getUsername());
        assertEquals(0, kevin.getFriendIDs().size());
        jordan = accountRepository.findByUsername(jordan.getUsername());
        assertEquals(1, jordan.getFriendIDs().size());
        assertEquals(0, kevin.getMessageIDs().size());

        invite = accountService.sendFriendInvite(jordan.getAccountID(), shawn.getAccountID());
        assertNull(invite);
        assertEquals(false, messageRepository.existsByMessageID(first));
        assertEquals(false, messageRepository.existsByMessageID(second));
    }

    /* Back end Unit Test for User Story 15 */
    @Test
    public void testReputationSuggestions() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account kevin = accountService.createAccount("knagar", "montgo38@pdue.edu", "Kevin", "Nagar");
        Account buckmaster = accountService.createAccount("buckmast", "buckmast@pdue.edu", "Jordan", "Buckmaster");
        Account rhys = accountService.createAccount("rbuckmas", "rbuckmas@pdue.edu", "Rhys", "Buckmaster");

        jordan.setReputation(55);
        shawn.setReputation(59);
        kevin.setReputation(49);
        buckmaster.setReputation(69);
        rhys.setReputation(45);

        accountRepository.save(jordan);
        accountRepository.save(shawn);
        accountRepository.save(kevin);
        accountRepository.save(buckmaster);
        accountRepository.save(rhys);

        List<Account> results = accountService.searchByReputation(80, false, false);
        assertEquals(true, results.isEmpty());

        results = accountService.searchByReputation(69, true, false);
        assertEquals(1, results.size());
        assertEquals("Jordan Buckmaster", results.get(0).getFullName());

        for(int i = 0; i < 10; i++) {
            results = accountService.searchByReputation(45, true, false);
            assertEquals(6, results.size());
            assertNull(results.get(1));
            assertEquals(true, (results.get(0).getReputation() <= 55));
            assertEquals("Jordan Buckmaster", results.get(2).getFullName());
            assertEquals("Shawn Montgomery", results.get(3).getFullName());
        }
    }

    /* Back end Unit Test for User Story 16 */
    @Test
    public void testOptingOutOfSearch() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "UniqueJordan", "Reed");
        Account buckmaster = accountService.createAccount("buckmast", "buckmast@pdue.edu", "UniqueJordan", "Buckmaster");
        Account rhys = accountService.createAccount("rbuckmas", "rbuckmas@pdue.edu", "Rhys", "Buckmaster");

        jordan.setReputation(55);
        jordan.setOptedOut(true);
        buckmaster.setReputation(45);
        rhys.setReputation(59);

        accountRepository.save(jordan);
        accountRepository.save(buckmaster);
        accountRepository.save(rhys);

        List<Account> results = accountService.searchByFirstName("UniqueJordan");
        assertEquals(1, results.size());

        Map<String, Account> searchMap = accountService.searchByFullName("UniqueJordan Reed");
        assertEquals(0, searchMap.size());

        results = accountService.searchByLastName("Reed");
        assertEquals(0, results.size());

        results = accountService.searchByReputation(50, false, false);
        assertEquals(1, results.size());
        assertEquals("Rhys Buckmaster", results.get(0).getFullName());
    }

    /* Back end Unit Test for User Story 17 */
    @Test
    public void testReputationRange() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account buckmaster = accountService.createAccount("buckmast", "buckmast@pdue.edu", "Jordan", "Buckmaster");

        jordan.setReputation(40);
        shawn.setReputation(80);
        shawn.setReputationReq(0.75);
        buckmaster.setReputation(60);
        buckmaster.setReputationReq(1.0);

        accountRepository.save(jordan);
        accountRepository.save(shawn);
        accountRepository.save(buckmaster);

        List<Account> results = accountService.searchByReputation(60, false, false);
        assertEquals(2, results.size());

        results = accountService.searchByReputation(0, false, false);
        assertEquals(1, results.size());

        results = accountService.searchByReputation(59, false, false);
        assertEquals(0, results.size());
    }

    /* Back end Unit Test for User Story 20 */
    @Test
    public void testNewUserFlag() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account kevin = accountService.createAccount("knagar", "knagar@pdue.edu", "Kevin", "Nagar");
        Account buckmaster = accountService.createAccount("buckmast", "buckmast@pdue.edu", "Jordan", "Buckmaster");

        jordan.setReputation(40);
        jordan.setLoggedInTime(Account.NEW_USER_HOURS*30);
        shawn.setReputation(80);
        shawn.setNewUser(false);
        kevin.setReputation(40);
        kevin.setLoggedInTime(Account.NEW_USER_HOURS*45);
        buckmaster.setReputation(60);
        buckmaster.setLoggedInTime(Account.NEW_USER_HOURS*60 + 1);

        accountRepository.save(jordan);
        accountRepository.save(shawn);
        accountRepository.save(kevin);
        accountRepository.save(buckmaster);

        String result = accountService.checkNewUserFlag("reed226");
        assertEquals("New User", result);

        result = accountService.checkNewUserFlag("montgo38");
        assertEquals("Experienced User", result);

        result = accountService.checkNewUserFlag("knagar");
        assertEquals("New User", result);

        result = accountService.checkNewUserFlag("buckmast");
        assertEquals("Experienced User", result);

        jordan = accountRepository.findByUsername(jordan.getUsername());
        shawn = accountRepository.findByUsername(shawn.getUsername());
        kevin = accountRepository.findByUsername(kevin.getUsername());
        buckmaster = accountRepository.findByUsername(buckmaster.getUsername());

        assertEquals(65, jordan.getAdjustedReputation());
        assertEquals(80, shawn.getAdjustedReputation());
        assertEquals(52, kevin.getAdjustedReputation());
        assertEquals(60, buckmaster.getAdjustedReputation());

        List<Account> accounts = accountService.searchByReputation(60, false, true);
        assertEquals(3, accounts.size());

        accounts = accountService.searchByReputation(52, false, true);
        assertEquals(4, accounts.size());
    }

    /* Back end Unit Test for User Story 23 */
    @Test
    public void testPublicPrivateGroupSearch() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account kevin = accountService.createAccount("knagar", "knagar@pdue.edu", "Kevin", "Nagar");

        Group cliquer = groupService.createGroup(
                "Cliquer",
                "To create a web app that facilitates the teaming of people who may have never met before",
                jordan.getAccountID());
        Group hoops = groupService.createGroup(
                "Hoops",
                "To play basketball",
                shawn.getAccountID());
        Group games = groupService.createGroup(
                "Games",
                "To play video games",
                shawn.getAccountID());
        cliquer.setReputationReq(0.01);
        hoops.setReputationReq(0.01);
        games.setReputationReq(0.01);

        jordan.setLatitude(40.00);
        jordan.setLongitude(-80.00);
        jordan.setProximityReq(Integer.MAX_VALUE);
        jordan.setReputation(80);
        jordan.setReputationReq(0.01);
        shawn.setLatitude(40.2);
        shawn.setLongitude(-80.4);
        shawn.setProximityReq(Integer.MAX_VALUE);
        shawn.setReputation(80);
        shawn.setReputationReq(0.01);
        kevin.setLatitude(40.4);
        kevin.setLongitude(-80.8);
        kevin.setProximityReq(Integer.MAX_VALUE);
        kevin.setReputation(80);
        kevin.setReputationReq(0.01);

        accountRepository.save(jordan);
        accountRepository.save(shawn);
        accountRepository.save(kevin);

        groupService.updateGroupSettings(cliquer.getGroupID(), cliquer.getGroupLeaderID(), "isPublic", "false");
        groupService.updateGroupSettings(hoops.getGroupID(), hoops.getGroupLeaderID(), "isPublic", "true");
        groupService.updateGroupSettings(games.getGroupID(), games.getGroupLeaderID(), "isPublic", "true");

        List<Group> groups = groupService.searchBySettings("knagar", null);
        assertEquals(2, groups.size());
        assertEquals("Games", groups.get(0).getGroupName());
        assertEquals("Hoops", groups.get(1).getGroupName());
    }

    /* Back end Unit Test for User Story 24 */
    @Test
    public void testGroupClosing() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account kevin = accountService.createAccount("knagar", "knagar@pdue.edu", "Kevin", "Nagar");

        Group cliquer = groupService.createGroup(
                "Cliquer",
                "To create a web app that facilitates the teaming of people who may have never met before",
                jordan.getAccountID());
        groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), shawn.getAccountID());
        groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), kevin.getAccountID());

        Group hoops = groupService.createGroup(
                "Hoops",
                "Play ball",
                kevin.getAccountID());
        groupService.addGroupMember(hoops.getGroupID(), kevin.getAccountID(), jordan.getAccountID());

        Group group = groupService.deleteGroup(cliquer.getGroupID(), jordan.getAccountID());
        assertNotNull(group);
        Group retrieve = groupService.getUserGroup(cliquer.getGroupID(), jordan.getAccountID());
        assertNull(retrieve);

        Account account = accountRepository.findByUsername(shawn.getUsername());
        assertEquals(0, account.getGroupIDs().size());
        account = accountRepository.findByUsername(kevin.getUsername());
        assertEquals(1, account.getGroupIDs().size());
    }

    /* Back end Unit Test for User Story 25 */
    @Test
    public void testGroupMemberKicking() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account kevin = accountService.createAccount("knagar", "knagar@pdue.edu", "Kevin", "Nagar");
        Account buckmaster = accountService.createAccount("buckmast", "buckmast@pdue.edu", "Jordan", "Buckmaster");
        Account rhys = accountService.createAccount("rbuckmas", "rbuckmas@pdue.edu", "Rhys", "Buckmaster");

        Group cliquer = groupService.createGroup(
                "Cliquer",
                "To create a web app that facilitates the teaming of people who may have never met before",
                jordan.getAccountID());
        groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), shawn.getAccountID());
        groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), kevin.getAccountID());
        groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), buckmaster.getAccountID());
        groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), rhys.getAccountID());

        Group result = groupService.acceptVoteKick(cliquer.getGroupID(), buckmaster.getAccountID());
        assertNull(result);

        result = groupService.startVoteKick(cliquer.getGroupID(), jordan.getAccountID(), kevin.getAccountID());
        assertEquals(kevin.getAccountID(), result.getKickCandidate());

        result = groupService.acceptVoteKick(cliquer.getGroupID(), kevin.getAccountID());
        assertNull(result);

        result = groupService.acceptVoteKick(cliquer.getGroupID(), buckmaster.getAccountID());
        assertEquals(false, result.getGroupMemberIDs().containsKey(kevin.getAccountID()));
        kevin = accountRepository.findByUsername(kevin.getUsername());
        assertEquals(0, kevin.getGroupIDs().size());

        result = groupService.acceptVoteKick(cliquer.getGroupID(), rhys.getAccountID());
        assertNull(result);

        result = groupService.startVoteKick(cliquer.getGroupID(), jordan.getAccountID(), rhys.getAccountID());
        assertEquals(rhys.getAccountID(), result.getKickCandidate());

        result = groupService.closeVoteKick(cliquer.getGroupID(), jordan.getAccountID());
        assertNull(result.getKickCandidate());

        result = groupService.removeGroupMember(cliquer.getGroupID(), jordan.getAccountID(), rhys.getAccountID());
        assertEquals(false, result.getGroupMemberIDs().containsKey(rhys.getAccountID()));
        rhys = accountRepository.findByUsername(rhys.getUsername());
        assertEquals(0, rhys.getGroupIDs().size());
    }

    /* Back end Unit Test for User Story 28 */
    @Test
    public void testJoiningGroup() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account kevin = accountService.createAccount("knagar", "knagar@pdue.edu", "Kevin", "Nagar");
        Account buckmaster = accountService.createAccount("buckmast", "buckmast@pdue.edu", "Jordan", "Buckmaster");
        Account rhys = accountService.createAccount("rbuckmas", "rbuckmas@pdue.edu", "Rhys", "Buckmaster");

        Group cliquer = groupService.createGroup(
                "Cliquer",
                "To create a web app that facilitates the teaming of people who may have never met before",
                jordan.getAccountID());

        Skill requirement = skillRepository.findBySkillNameAndSkillLevel("JavaScript", 7);
        Skill meetReq = skillRepository.findBySkillNameAndSkillLevel("JavaScript", 8);
        Skill missReq = skillRepository.findBySkillNameAndSkillLevel("JavaScript", 6);

        cliquer.setReputationReq(0.5);
        cliquer.setProximityReq(30);
        cliquer.addSkillReq(requirement);
        jordan.setNewUser(false);
        jordan.setReputation(50);
        jordan.setLatitude(40.0);
        jordan.setLongitude(-80.0);
        jordan.setProximityReq(10);
        jordan.addSkill(meetReq);

        // Tests when member tries joining a group they are in
        shawn.setNewUser(false);
        shawn.setLatitude(40.0);
        shawn.setLongitude(-80.0);
        shawn.addSkill(meetReq);

        // Tests when joiner is too far away from the group leader
        kevin.setNewUser(false);
        kevin.setLatitude(40.4);
        kevin.setLongitude(-80.8);
        kevin.setReputation(40);
        kevin.addSkill(meetReq);

        // Tests when a joiner satisfies all requirements, then tests if joiner lacks reputation
        buckmaster.setNewUser(false);
        buckmaster.setLatitude(40.2);
        buckmaster.setLongitude(-80.4);
        buckmaster.setReputation(40);
        buckmaster.addSkill(meetReq);

        // Tests when a joiner lacks the skill requirement
        rhys.setNewUser(false);
        rhys.setLatitude(40.0);
        rhys.setLongitude(-80.0);
        rhys.setReputation(35);
        rhys.addSkill(missReq);

        groupRepository.save(cliquer);
        accountRepository.save(jordan);
        accountRepository.save(shawn);
        accountRepository.save(kevin);
        accountRepository.save(buckmaster);
        accountRepository.save(rhys);

        groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), shawn.getAccountID());

        Message result = groupService.requestToJoinGroup(cliquer.getGroupID(), shawn.getAccountID());
        assertNull(result);

        result = groupService.requestToJoinGroup(cliquer.getGroupID(), kevin.getAccountID());
        assertNull(result);

        result = groupService.requestToJoinGroup(cliquer.getGroupID(), buckmaster.getAccountID());
        assertEquals("User Jordan Buckmaster wishes to join your group Cliquer", result.getContent());

        result = groupService.requestToJoinGroup(cliquer.getGroupID(), rhys.getAccountID());
        assertNull(result);

        buckmaster.setReputation(20);
        accountRepository.save(buckmaster);
        result = groupService.requestToJoinGroup(cliquer.getGroupID(), buckmaster.getAccountID());
        assertNull(result);

        jordan = accountRepository.findByUsername(jordan.getUsername());
        assertEquals(1, jordan.getMessageIDs().size());
        List<String> messageIDs = new ArrayList<>(jordan.getMessageIDs().keySet());
        Message message = messageRepository.findByMessageID(messageIDs.get(0));
        assertEquals(buckmaster.getAccountID(), message.getSenderID());
        assertEquals(cliquer.getGroupID(), message.getGroupID());

        result = groupService.denyJoinRequest(jordan.getAccountID(), message.getMessageID());
        assertEquals(null, messageRepository.findByMessageID(result.getMessageID()));
        jordan = accountRepository.findByUsername(jordan.getUsername());
        assertEquals(0, jordan.getMessageIDs().size());

        buckmaster = accountRepository.findByUsername(buckmaster.getUsername());
        assertEquals(0, buckmaster.getMessageIDs().size());
        assertEquals(0, buckmaster.getGroupIDs().size());

        buckmaster.setMessageIDs(new TreeMap<>());
        buckmaster.setReputation(40);
        accountRepository.save(buckmaster);
        result = groupService.requestToJoinGroup(cliquer.getGroupID(), buckmaster.getAccountID());
        assertEquals("User Jordan Buckmaster wishes to join your group Cliquer", result.getContent());

        jordan = accountRepository.findByUsername(jordan.getUsername());
        messageIDs = new ArrayList<>(jordan.getMessageIDs().keySet());
        message = messageRepository.findByMessageID(messageIDs.get(0));
        result = groupService.acceptJoinRequest(jordan.getAccountID(), message.getMessageID());
        //assertEquals("You have been accepted into group Cliquer", result.getContent());
        jordan = accountRepository.findByUsername(jordan.getUsername());
        assertEquals(0, jordan.getMessageIDs().size());

        buckmaster = accountRepository.findByUsername(buckmaster.getUsername());
        //assertEquals(1, buckmaster.getMessageIDs().size());
        assertEquals(1, buckmaster.getGroupIDs().size());
        assertEquals(cliquer.getGroupName(), buckmaster.getGroupIDs().get(cliquer.getGroupID()));

        cliquer = groupRepository.findByGroupID(cliquer.getGroupID());
        assertEquals(true, cliquer.getGroupMemberIDs().containsKey(buckmaster.getAccountID()));
    }

    /* Back end Unit Test for User Story 30 */
    @Test
    public void testChatHistory() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account kevin = accountService.createAccount("knagar", "knagar@pdue.edu", "Kevin", "Nagar");

        Group cliquer = groupService.createGroup(
                "Cliquer",
                "To create a web app that facilitates the teaming of people who may have never met before",
                jordan.getAccountID());

        groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), kevin.getAccountID());

        accountService.sendMessage(jordan.getAccountID(), cliquer.getGroupID(), "Hello", Types.CHAT_MESSAGE);
        accountService.sendMessage(kevin.getAccountID(), cliquer.getGroupID(), "Hey", Types.CHAT_MESSAGE);
        accountService.sendMessage(jordan.getAccountID(), cliquer.getGroupID(), "Bye", Types.CHAT_MESSAGE);

        List<Message> messages = accountService.getChatHistory(cliquer.getGroupID(), jordan.getUsername());

        assertEquals(3, messages.size());

        assertEquals("Hello", messages.get(0).getContent());
        assertEquals("Hey", messages.get(1).getContent());
        assertEquals("Bye", messages.get(2).getContent());
    }

    /* Back end Unit Test for User Story 32 */
    @Test
    public void testSkillRating() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account kevin = accountService.createAccount("knagar", "knagar@pdue.edu", "Kevin", "Nagar");

        Group cliquer = groupService.createGroup(
                "Cliquer",
                "To create a web app that facilitates the teaming of people who may have never met before",
                jordan.getAccountID());

        Skill java = skillRepository.findBySkillNameAndSkillLevel("Java", 7);
        Skill vim = skillRepository.findBySkillNameAndSkillLevel("VIM", 5);

        assertNotNull(groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), shawn.getAccountID()));
        assertNotNull(groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), kevin.getAccountID()));

        cliquer = groupRepository.findByGroupID(cliquer.getGroupID());
        jordan = accountRepository.findByAccountID(jordan.getAccountID());
        shawn = accountRepository.findByAccountID(shawn.getAccountID());
        kevin = accountRepository.findByAccountID(kevin.getAccountID());

        cliquer.addSkillReq(java);
        cliquer.addSkillReq(vim);
        groupRepository.save(cliquer);
        
        jordan.addSkill(java);
        jordan.addSkill(vim);
        accountRepository.save(jordan);

        shawn.addSkill(java);
        shawn.addSkill(vim);
        accountRepository.save(shawn);

        kevin.addSkill(java);
        kevin.addSkill(vim);
        accountRepository.save(kevin);
        
        groupService.initiateRatings(cliquer.getGroupID(), jordan.getAccountID());

        Map<String, Integer> form = groupService.getGroupMemberRatingForm(cliquer.getGroupID(), kevin.getAccountID());
        assertNotNull(form);
        form.replace("Java", 5);
        form.replace("VIM", 7);
        String result = groupService.rateGroupMember(cliquer.getGroupID(), shawn.getAccountID(), kevin.getAccountID(), false, form);
        assertNotNull(result);
        Skill newJava = skillRepository.findBySkillNameAndSkillLevel("Java", 5);
        Skill newVim = skillRepository.findBySkillNameAndSkillLevel("VIM", 7);
        kevin = accountRepository.findByAccountID(kevin.getAccountID());
        assertEquals(true, kevin.getSkillIDs().containsKey(newJava.getSkillID()));
        assertEquals(true, kevin.getSkillIDs().containsKey(newVim.getSkillID()));
        assertEquals(false, kevin.getSkillIDs().containsKey(java.getSkillID()));

        form = groupService.getGroupMemberRatingForm(cliquer.getGroupID(), kevin.getAccountID());
        assertNotNull(form);
        form.replace("Java", 5);
        form.replace("VIM", 7);
        result = groupService.rateGroupMember(cliquer.getGroupID(), shawn.getAccountID(), kevin.getAccountID(), false, form);
        assertNull(result);

        form = groupService.getGroupMemberRatingForm(cliquer.getGroupID(), kevin.getAccountID());
        assertNotNull(form);
        form.replace("Java", 7);
        form.replace("VIM", 9);
        result = groupService.rateGroupMember(cliquer.getGroupID(), jordan.getAccountID(), kevin.getAccountID(), false, form);
        assertNotNull(result);
        newJava = skillRepository.findBySkillNameAndSkillLevel("Java", 6);
        newVim = skillRepository.findBySkillNameAndSkillLevel("VIM", 8);
        kevin = accountRepository.findByAccountID(kevin.getAccountID());
        assertEquals(true, kevin.getSkillIDs().containsKey(newJava.getSkillID()));
        assertEquals(true, kevin.getSkillIDs().containsKey(newVim.getSkillID()));
        assertEquals(false, kevin.getSkillIDs().containsKey(java.getSkillID()));

        form = groupService.getGroupMemberRatingForm(cliquer.getGroupID(), jordan.getAccountID());
        assertNotNull(form);
        form.replace("Java", 8);
        result = groupService.rateGroupMember(cliquer.getGroupID(), shawn.getAccountID(), jordan.getAccountID(), false, form);
        assertNotNull(result);
        newJava = skillRepository.findBySkillNameAndSkillLevel("Java", 8);
        jordan = accountRepository.findByAccountID(jordan.getAccountID());
        assertEquals(true, jordan.getSkillIDs().containsKey(newJava.getSkillID()));
        assertEquals(true, jordan.getSkillIDs().containsKey(vim.getSkillID()));
        assertEquals(false, jordan.getSkillIDs().containsKey(java.getSkillID()));
    }

    /* Back end Unit Test for User Story 33 */
    @Test
    public void testReputationRating() {
        Account jordan = accountService.createAccount("reed226", "reed226@pdue.edu", "Jordan", "Reed");
        Account shawn = accountService.createAccount("montgo38", "montgo38@pdue.edu", "Shawn", "Montgomery");
        Account kevin = accountService.createAccount("knagar", "knagar@pdue.edu", "Kevin", "Nagar");

        Group cliquer = groupService.createGroup(
                "Cliquer",
                "To create a web app that facilitates the teaming of people who may have never met before",
                jordan.getAccountID());

        assertNotNull(groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), shawn.getAccountID()));
        assertNotNull(groupService.addGroupMember(cliquer.getGroupID(), jordan.getAccountID(), kevin.getAccountID()));

        cliquer = groupRepository.findByGroupID(cliquer.getGroupID());
        jordan = accountRepository.findByAccountID(jordan.getAccountID());
        shawn = accountRepository.findByAccountID(shawn.getAccountID());
        kevin = accountRepository.findByAccountID(kevin.getAccountID());


        jordan.setReputation(40);
        accountRepository.save(jordan);

        shawn.setReputation(99);
        accountRepository.save(shawn);

        kevin.setReputation(1);
        accountRepository.save(kevin);

        groupService.initiateRatings(cliquer.getGroupID(), jordan.getAccountID());

        Map<String, Integer> form = groupService.getGroupMemberRatingForm(cliquer.getGroupID(), kevin.getAccountID());
        assertNotNull(form);
        String result = groupService.rateGroupMember(cliquer.getGroupID(), shawn.getAccountID(), kevin.getAccountID(), true, form);
        assertNotNull(result);
        kevin = accountRepository.findByAccountID(kevin.getAccountID());
        assertEquals(9, kevin.getReputation());

        form = groupService.getGroupMemberRatingForm(cliquer.getGroupID(), kevin.getAccountID());
        assertNotNull(form);
        result = groupService.rateGroupMember(cliquer.getGroupID(), shawn.getAccountID(), kevin.getAccountID(), true, form);
        assertNull(result);

        form = groupService.getGroupMemberRatingForm(cliquer.getGroupID(), kevin.getAccountID());
        assertNotNull(form);
        result = groupService.rateGroupMember(cliquer.getGroupID(), jordan.getAccountID(), kevin.getAccountID(), true, form);
        assertNotNull(result);
        kevin = accountRepository.findByAccountID(kevin.getAccountID());
        assertEquals(13, kevin.getReputation());

        form = groupService.getGroupMemberRatingForm(cliquer.getGroupID(), jordan.getAccountID());
        assertNotNull(form);
        result = groupService.rateGroupMember(cliquer.getGroupID(), kevin.getAccountID(), jordan.getAccountID(), true, form);
        assertNotNull(result);
        jordan = accountRepository.findByAccountID(jordan.getAccountID());
        assertEquals(42, jordan.getReputation());

        form = groupService.getGroupMemberRatingForm(cliquer.getGroupID(), shawn.getAccountID());
        assertNotNull(form);
        result = groupService.rateGroupMember(cliquer.getGroupID(), jordan.getAccountID(), shawn.getAccountID(), true, form);
        assertNotNull(result);
        shawn = accountRepository.findByAccountID(shawn.getAccountID());
        assertEquals(100, shawn.getReputation());
    }

    /* Populates valid skills into database, in case they were deleted */
    @Before
    public void populateSkills() {
        accountService.addSkillToDatabase("Java");
        accountService.addSkillToDatabase("JavaScript");
        accountService.addSkillToDatabase("C");
        accountService.addSkillToDatabase("C++");
        accountService.addSkillToDatabase("Python");
        accountService.addSkillToDatabase("C#");
        accountService.addSkillToDatabase("Ruby");
        accountService.addSkillToDatabase("Pascal");
        accountService.addSkillToDatabase("ARM");
        accountService.addSkillToDatabase("x86");
        accountService.addSkillToDatabase("Verilog");
        accountService.addSkillToDatabase("VIM");
        accountService.addSkillToDatabase("Microsoft Word");
        accountService.addSkillToDatabase("Google Sheets");
        accountService.addSkillToDatabase("Swift");
        accountService.addSkillToDatabase("Real Time Strategy Games");
        accountService.addSkillToDatabase("Role-Playing Games");
        accountService.addSkillToDatabase("Board Games");
        accountService.addSkillToDatabase("Platformer Games");
        accountService.addSkillToDatabase("Massively Multiplayer Online Role-Playing Games");
        accountService.addSkillToDatabase("Basketball");
        accountService.addSkillToDatabase("Lifting");
        accountService.addSkillToDatabase("Football");
        accountService.addSkillToDatabase("Volleyball");
        accountService.addSkillToDatabase("Baseball");
        accountService.addSkillToDatabase("Soccer");
        accountService.addSkillToDatabase("Tennis");
        accountService.addSkillToDatabase("Really Long Skill Name That Likely Needs To Be Shortened When It Is Shown On The Front End");
    }

    /* Function to clear items that should not already be in database */
    @After
    public void clearDatabase() {
        accountRepository.deleteAll();
        skillRepository.deleteAll();
        messageRepository.deleteAll();
        groupRepository.deleteAll();
    }
}
