package com.sep.vox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

@ImportGrpcClients(basePackageClasses = VoxApplication.class)
@SpringBootApplication
public class VoxApplication {

	public static void main(String[] args) {
		SpringApplication.run(VoxApplication.class, args);
	}

}
