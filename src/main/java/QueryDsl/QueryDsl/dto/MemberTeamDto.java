package QueryDsl.QueryDsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;


@Data
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private int age;
    private Long teamId;
    private String teamName;


    //queryDsl 사용 - > gradle에서 compileQueryDsl 해줘야한다.
    @QueryProjection
    public MemberTeamDto(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
