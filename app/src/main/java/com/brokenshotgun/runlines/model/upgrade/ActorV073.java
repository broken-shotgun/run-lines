package com.brokenshotgun.runlines.model.upgrade;

import com.brokenshotgun.runlines.model.Actor;
import com.google.gson.annotations.SerializedName;

/**
 * v073 classes exist due to a Proguard error in v0.7.3
 * <p>
 * An incorrect package name to ignore was specified in the Proguard rules,
 * so all the model files got obfuscated.
 *
 * @deprecated use {@link com.brokenshotgun.runlines.model.Actor}
 */
final class ActorV073 {
    @SerializedName("b")
    public String name;

    Actor convert() {
        return new Actor(name);
    }
}
