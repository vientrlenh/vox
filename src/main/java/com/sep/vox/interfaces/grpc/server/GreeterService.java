package com.sep.vox.interfaces.grpc.server;

import org.springframework.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;

import com.sep.vox.grpc.proto.GreetReply;
import com.sep.vox.grpc.proto.GreetRequest;
import com.sep.vox.grpc.proto.GreeterGrpc.GreeterImplBase;

import io.grpc.stub.StreamObserver;

@GrpcService
public class GreeterService extends GreeterImplBase {
    
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void greet(GreetRequest request, StreamObserver<GreetReply> responseObserver) {
        var reply = GreetReply.newBuilder()
            .setMessage("Hello " + request.getName())
            .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void streamGreet(GreetRequest request, StreamObserver<GreetReply> responseObserver) {
        var count = 1;
        while (count <= 10) {
            var reply = GreetReply.newBuilder()
                .setMessage("Hello (" + count + "): " + request.getName())
                .build();
            responseObserver.onNext(reply);
            count++;
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                responseObserver.onError(e);
                return;
            }
        }
        responseObserver.onCompleted();
    }
}
