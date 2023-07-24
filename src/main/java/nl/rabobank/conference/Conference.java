package nl.rabobank.conference;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value(staticConstructor="of")
public class Conference
{
    String name;
    String country;
    String city;
    String date;

    public void handleCFP(String name)
    {
        log.info("{} has applied for {}", name, this.getName());
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Conference(@JsonProperty("name") final String name,
                      @JsonProperty("country") final String country,
                      @JsonProperty("city") final String city,
                      @JsonProperty("date") final String date)
    {
        this.name = name;
        this.country = country;
        this.city = city;
        this.date = date;
    }

}
