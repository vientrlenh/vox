package com.sep.vox.application.port.input.usecase;

public interface IUseCase<I, O> {
    
    O execute(I input);
}
