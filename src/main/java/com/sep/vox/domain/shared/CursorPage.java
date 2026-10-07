package com.sep.vox.domain.shared;

import java.util.List;
import java.util.UUID;

public record CursorPage<T>(
    List<T> content, 
    UUID nextCursor, 
    boolean hasNext
) {
    
}
