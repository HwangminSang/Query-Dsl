package QueryDsl.QueryDsl.repository;

import QueryDsl.QueryDsl.dto.MemberSearchCondition;
import QueryDsl.QueryDsl.dto.MemberTeamDto;
import QueryDsl.QueryDsl.dto.QMemberTeamDto;
import QueryDsl.QueryDsl.entity.Member;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static QueryDsl.QueryDsl.entity.QMember.member;
import static QueryDsl.QueryDsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

/**
 * QuerydslRepositroySupport 사용하면 페이지를 대신해준다.
 */
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {


    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public List<MemberTeamDto> search(MemberSearchCondition memberSearchCondition) {

        return jpaQueryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.Id.as("teamId"),
                        team.name.as("teamName"))
                ).from(member)
                .leftJoin(member.team, team)  //team의 데이터를 다가져오기위해서 leftJoin
                .where(usernameEq(memberSearchCondition.getUsername()),
                        teamNameEq(memberSearchCondition.getTeamName()),
                        ageBetween(memberSearchCondition.getAgeGoe(), memberSearchCondition.getAgeLoe())
                ).fetch();

    }


    private BooleanExpression ageBetween(int ageGoe, int ageLog) {

        return ageGoe(ageGoe).and(ageLoe(ageLog));
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    /**
     * 페이지
     * Pageable pageable 파라미터로 받는다.
     * offset()메서드 이용 몇번째부터 시작할것인지    pageable.getOffset()
     * limit()는 몇개를 볼것인가 .                 pageable.getPageSize()
     * PageRequest.of()으로 넣어주면 인터페이스인 Pageable로 받을수잇다.
     */

    // contents만 가져오고
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition memberSearchCondition, Pageable pageable) {

        List<MemberTeamDto> content = jpaQueryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.Id.as("teamId"),
                        team.name.as("teamName"))
                ).from(member)
                .leftJoin(member.team, team)  //team의 데이터를 다가져오기위해서 leftJoin
                .where(usernameEq(memberSearchCondition.getUsername()),
                        teamNameEq(memberSearchCondition.getTeamName()),
                        ageGoe(memberSearchCondition.getAgeGoe()),
                        ageLoe(memberSearchCondition.getAgeLoe())
                ).offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        //실제 내용

        int total = content.size();


        return new PageImpl<>(content, pageable, total);

    }

    /**
     * 최적화!!!!
     * ( 카운터를 보고 호출할지 안할지 정함 )
     * //countQuery.fetchCount()를 해야 count커리가 나옴
     * // countQuery::fetchOne  -- >  ()-> ~~ 같다
     * //여기서 판단하고 count를 날린다. 첫페이지 컨텐트 사이즈가 체크
     * // 즉 첫번째 페이지에서 모든 데어티를 다가져오면 다음페이지가 없기때문에 count커리 안나간다.
     * 사용
     * :PageableExecutionUtils.getPage
     *
     * @param memberSearchCondition
     * @param pageable
     * @return
     */
    @Override
    public Page<MemberTeamDto> searchPageCmplex(MemberSearchCondition memberSearchCondition, Pageable pageable) {


        //결과만
        List<MemberTeamDto> content = jpaQueryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.Id.as("teamId"),
                        team.name.as("teamName"))
                ).from(member)
                .leftJoin(member.team, team)  //team의 데이터를 다가져오기위해서 leftJoin
                .where(usernameEq(memberSearchCondition.getUsername()),
                        teamNameEq(memberSearchCondition.getTeamName()),
                        ageGoe(memberSearchCondition.getAgeGoe()),
                        ageLoe(memberSearchCondition.getAgeLoe())
                ).offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        //카운터 쿼리 분할
        JPAQuery<Long> countQuery =
                jpaQueryFactory
                        .select(member.count())
                        .from(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(memberSearchCondition.getUsername()),
                                teamNameEq(memberSearchCondition.getTeamName()),
                                ageGoe(memberSearchCondition.getAgeGoe()),
                                ageLoe(memberSearchCondition.getAgeLoe())
                        );


        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);


    }


}
