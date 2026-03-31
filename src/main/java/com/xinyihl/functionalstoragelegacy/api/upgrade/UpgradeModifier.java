package com.xinyihl.functionalstoragelegacy.api.upgrade;

import java.util.Collections;
import java.util.List;

/**
 * Defines how an upgrade modifies a particular aspect of a drawer.
 * Each modifier can alter a base value and/or a multiplicative factor.
 * <p>
 * Final value = modifiedBase * modifiedFactor
 */
public interface UpgradeModifier {

    default float modifyBase(float base) {
        return base;
    }

    default float modifyFactor(float factor) {
        return factor;
    }

    static float calculate(List<UpgradeModifier> modifiers, float defaultBase) {
        float base = defaultBase;
        float factor = 1.0f;
        for (UpgradeModifier mod : modifiers) {
            base = mod.modifyBase(base);
            factor = mod.modifyFactor(factor);
        }
        return Math.max(base * factor, 0f);
    }

    static float calculate(float defaultBase) {
        return Math.max(defaultBase, 0f);
    }

    class MultiplyFactor implements UpgradeModifier {
        private final float factor;

        public MultiplyFactor(float factor) {
            this.factor = factor;
        }

        @Override
        public float modifyFactor(float f) {
            return f * factor;
        }

        public float getFactor() {
            return factor;
        }
    }

    class SetBase implements UpgradeModifier {
        private final float amount;

        public SetBase(float amount) {
            this.amount = amount;
        }

        @Override
        public float modifyBase(float base) {
            return amount;
        }

        public float getAmount() {
            return amount;
        }
    }

    class AddToBase implements UpgradeModifier {
        private final float amount;

        public AddToBase(float amount) {
            this.amount = amount;
        }

        @Override
        public float modifyBase(float base) {
            return base + amount;
        }

        public float getAmount() {
            return amount;
        }
    }
}
