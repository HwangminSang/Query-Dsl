package QueryDsl.QueryDsl.entity;

import lombok.*;

import javax.persistence.*;


@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "age", "username"}) //team 적으면 안된다.
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    //지연로딩!!!!!!!!!!!
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // fk
    private Team team;


    public Member(String username, int age, Team team) {
        this.age = age;
        this.username = username;
        if (team != null) {
            changTeam(team);
        }
    }

    public Member(String username) {
        this(username, 0);

    }

    public Member(String username, int age) {

        this(username, age, null);
    }

    public void changTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }

}


