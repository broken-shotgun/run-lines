package com.brokenshotgun.runlines.model.upgrade;

import com.brokenshotgun.runlines.model.Script;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * v073 classes exist due to a Proguard error in v0.7.3
 * <p>
 * An incorrect package name to ignore was specified in the Proguard rules,
 * so all the model files got obfuscated.
 *
 * @deprecated use {@link com.brokenshotgun.runlines.model.Script}
 */
@Deprecated
public final class ScriptV073 {
    @SerializedName("a")
    public long id = -1L;
    @SerializedName("b")
    public String name;
    @SerializedName("c")
    public List<ActorV073> actors = new ArrayList<>();
    @SerializedName("d")
    public List<LineV073> lines = new ArrayList<>();
    @SerializedName("e")
    public List<SceneV073> scenes = new ArrayList<>();

    public Script convert() {
        Script converted = new Script(name);
        converted.id = id;
        for (ActorV073 actor : actors) {
            converted.addActor(actor.convert());
        }
        for (LineV073 line : lines) {
            converted.addLine(line.convert());
        }
        for (SceneV073 scene : scenes) {
            converted.addScene(scene.convert());
        }
        converted.defaultVoice = "";
        return converted;
    }
}
