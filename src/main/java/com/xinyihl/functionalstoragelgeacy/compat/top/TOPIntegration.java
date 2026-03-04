package com.xinyihl.functionalstoragelgeacy.compat.top;

import mcjty.theoneprobe.api.ITheOneProbe;
import java.util.function.Function;

public class TOPIntegration implements Function<ITheOneProbe, Void> {

    @Override
    public Void apply(ITheOneProbe registrar) {
        registrar.registerProvider(new TileTOPDataProvider());
        return null;
    }
}
