package com.adam.fileprocessor;

import com.adam.fileprocessor.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class FileProcessorApplication {

	public static void main(String[] args) throws InterruptedException {

		SpringApplication.run(FileProcessorApplication.class, args);

		while (true) {
			Thread.sleep(10000);
		}

	}

	@Component
	@Profile("!TestNoWatcher")
	public class CommandLineAppStartupRunner implements CommandLineRunner {

		@Autowired
		private Environment env;

		@Autowired
		private BusinessService businessService;

		@Autowired
		private PlayerRepository playerRepository;


		@Override
		public void run(String...args) throws Exception {

			FileMonitor watcher = new FileMonitor(env, businessService, playerRepository);
			watcher.run();

		}
	}

}
