
package com.styxxco.cliquer.database;

import com.styxxco.cliquer.domain.Account;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.ArrayList;

public interface AccountRepository extends MongoRepository<Account, String>
{
	boolean existsByUsername(String username);
	Account findByUsername(String username);
	boolean existsByAccountID(ObjectId accountID);
	Account findByAccountID(ObjectId accountID);
	ArrayList<Account> findByFirstName(String firstName);
	ArrayList<Account> findByLastName(String firstName);
}