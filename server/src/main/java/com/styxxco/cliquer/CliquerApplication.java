package com.styxxco.cliquer;

import com.styxxco.cliquer.database.AccountRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class CliquerApplication {

	@Autowired private AccountRepository accountRepository;

	public static void main(String[] args) {
		SpringApplication.run(CliquerApplication.class, args);
	}
}