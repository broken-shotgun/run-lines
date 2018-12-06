package com.brokenshotgun.runlines.model.upgrade;

import com.brokenshotgun.runlines.model.Scene;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * v073 classes exist due to a Proguard error in v0.7.3
 * <p>
 * An incorrect package name to ignore was specified in the Proguard rules,
 * so all the model files got obfuscated.
 *
 * @deprecated use {@link com.brokenshotgun.runlines.model.Scene}
 */
final class SceneV073 {
    @SerializedName("a")
    public String name;
    @SerializedName("b")
    public int number;
    @SerializedName("c")
    public List<LineV073> lines = new ArrayList<>();

    Scene convert() {
        Scene converted = new Scene(name);
        converted.setNumber(number);
        for (LineV073 line : lines) {
            converted.addLine(line.convert());
        }
        return converted;
    }
}
