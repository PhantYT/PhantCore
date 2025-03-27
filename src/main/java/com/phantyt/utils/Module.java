package com.phantyt.utils;

import com.phantyt.PhantCore;

public interface Module {
    String getName();
    void enable(PhantCore plugin); // Используем PhantCore
    void disable(PhantCore plugin); // Используем PhantCore
}