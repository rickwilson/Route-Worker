package core.thirdParty.aftership.classes;


import core.thirdParty.aftership.enums.ISO3Country;
import org.json.JSONException;
import org.json.JSONObject;

public class Checkpoint {

    /** Date and time of the tracking created. */
    private String createdAt;

    /** Date and time of the checkpoint, provided by courier. Value may be:
     Empty String,
     YYYY-MM-DD,
     YYYY-MM-DDTHH:MM:SS, or
     YYYY-MM-DDTHH:MM:SS+TIMEZONE */
    private String checkpointTime;

    /** Location info (if any) */
    private String city;

    /** Country ISO Alpha-3 (three letters) of the checkpoint */
    private ISO3Country countryISO3;

    /** Country name of the checkpoint, may also contain other location info. */
    private String countryName;

    /** Checkpoint message */
    private String message;

    /** Location info (if any) */
    private String state;

    /** Status of the checkpoint */
    private String tag;

    /** Location info (if any) */
    private String zip;

    /** Location info (if any) */
    private String location;

    /** Slug */
    private String slug;

    public Checkpoint(JSONObject checkPointJSON) throws JSONException{

        this.createdAt = checkPointJSON.isNull("created_at")?null:checkPointJSON.getString("created_at");
        this.checkpointTime = checkPointJSON.isNull("checkpoint_time")?null:checkPointJSON.getString("checkpoint_time");
        this.city = checkPointJSON.isNull("city")?null:checkPointJSON.getString("city");
        this.countryISO3 = checkPointJSON.isNull("country_iso3")?null: ISO3Country.valueOf(checkPointJSON.getString("country_iso3"));
        this.countryName = checkPointJSON.isNull("country_name")?null:checkPointJSON.getString("country_name");
        this.message = checkPointJSON.isNull("message")?null:checkPointJSON.getString("message");
        this.state = checkPointJSON.isNull("state")?null:checkPointJSON.getString("state");
        this.tag = checkPointJSON.isNull("tag")?null:checkPointJSON.getString("tag");
        this.zip = checkPointJSON.isNull("zip")?null:checkPointJSON.getString("zip");
        this.location = checkPointJSON.isNull("location")?null:checkPointJSON.getString("location");
        this.slug = checkPointJSON.isNull("slug")?null:checkPointJSON.getString("slug");

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\tCheckpoint{");
        sb.append((slug==null)?"":"\n\t\tslug="+slug);
        sb.append((createdAt==null)?"":"\n\t\tcreatedAt="+createdAt);
        sb.append((checkpointTime==null)?"":"\n\t\tcheckpointTime="+checkpointTime);
        sb.append((city==null)?"":"\n\t\tcity="+city);
        sb.append((countryISO3==null)?"":"\n\t\tcountryISO3="+countryISO3);
        sb.append((countryName==null)?"":"\n\t\tcountryName="+countryName);
        sb.append((message==null)?"":"\n\t\tmessage="+message);
        sb.append((state==null)?"":"\n\t\tstate="+state);
        sb.append((tag==null)?"":"\n\t\ttag="+tag);
        sb.append((zip==null)?"":"\n\t\tzip="+zip);
        sb.append((location==null)?"":"\n\t\tlocation="+location);

        return sb.toString();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getCheckpointTime() {
        return checkpointTime;
    }

    public String getCity() {
        return city;
    }

    public ISO3Country getCountryISO3() {
        return countryISO3;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getMessage() {
        return message;
    }

    public String getState() {
        return state;
    }

    public String getTag() {
        return tag;
    }

    public String getZip() {
        return zip;
    }

    public String getLocation() {
        return location;
    }

    public String getSlug() {
        return slug;
    }
}



