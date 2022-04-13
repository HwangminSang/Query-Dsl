package QueryDsl.QueryDsl.entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "name"}) //team 적으면 안된다.
public class Team {

    public Team(String teamname) {

        this.name = teamname;
    }

    @Id
    @GeneratedValue
    private Long Id;


    private String name;


    //여기서는 update 안됨 . 주인설정
    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<Member>();


}
