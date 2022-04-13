package QueryDsl.QueryDsl.dto;


import lombok.Data;

/**
 * 조건 검색 당담 dto
 * //회원명 ,팀명 ,나이 (ageGoe,ageLoe)
 */
@Data
public class MemberSearchCondition {


    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;

}

