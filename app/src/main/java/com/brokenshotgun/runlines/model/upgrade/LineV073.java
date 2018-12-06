package com.brokenshotgun.runlines.model.upgrade;

import com.brokenshotgun.runlines.model.Line;
import com.google.gson.annotations.SerializedName;

/**
 * v073 classes exist due to a Proguard error in v0.7.3
 * <p>
 * An incorrect package name to ignore was specified in the Proguard rules,
 * so all the model files got obfuscated.
 *
 * @deprecated use {@link com.brokenshotgun.runlines.model.Line}
 */
final class LineV073 {
    @SerializedName("c")
    public ActorV073 actor;
    @SerializedName("d")
    public String line;
    @SerializedName("a")
    public int order;

    Line convert() {
        Line converted = new Line(actor.convert(), line);
        converted.order = order;
        return converted;
    }
}
