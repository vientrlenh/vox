package com.sep.vox.application.port.input;

public interface IUseCase<I, O> {
    
    O execute(I input);
}
