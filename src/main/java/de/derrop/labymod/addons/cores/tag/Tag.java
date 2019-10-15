package de.derrop.labymod.addons.cores.tag;
/*
 * Created by derrop on 15.10.2019
 */

public class Tag {
    private TagType tagType;
    private String creator;
    private String name;
    private String tag;
    private long creationTime;

    public Tag(TagType tagType, String creator, String name, String tag, long creationTime) {
        this.tagType = tagType;
        this.creator = creator;
        this.name = name;
        this.tag = tag;
        this.creationTime = creationTime;
    }

    public TagType getTagType() {
        return tagType;
    }

    public String getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public long getCreationTime() {
        return creationTime;
    }
}
