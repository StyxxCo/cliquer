
package com.styxxco.cliquer.database;

import com.styxxco.cliquer.domain.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String>
{
	public Message findByMessageID(String messageID);
}