package QueryDsl.QueryDsl.repository;

import QueryDsl.QueryDsl.dto.MemberSearchCondition;
import QueryDsl.QueryDsl.dto.MemberTeamDto;
import QueryDsl.QueryDsl.dto.QMemberTeamDto;
import QueryDsl.QueryDsl.entity.Member;
import QueryDsl.QueryDsl.entity.QMember;
import QueryDsl.QueryDsl.entity.QTeam;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static QueryDsl.QueryDsl.entity.QMember.*;
import static QueryDsl.QueryDsl.entity.QTeam.*;
import static org.springframework.util.StringUtils.*;

/**
 * 순수 jpa vs QueryDsl 비교
 * 생성자에서 em을 injection하면 스프링에서 알아서 넣어준다.
 * JPAQueryFactory  queryDsl 사용할때 필요
 */
@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;


    public void save(Member member) {

        em.persist(member);
    }

    public Optional<Member> findById(long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);

    }

    //순수 jpa
    public List<Member> findAll() {
        return em.createQuery("select m from Member m ", Member.class).getResultList();

    }

    //QueryDSL
    public List<Member> findAll_Querydsl() {

        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    //순수 jpa
    public List<Member> findByUsername(String username) {

        return em.createQuery(
                        "select m from Member m where m.username = :username", Member.class
                ).setParameter("username", username)
                .getResultList();
    }

    //QueryDsl
    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }


    //동적커리 BooleanBuilder 이용
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        //StringUtils.hasText() 이용 !!!!   ""체크 and null 체크
        //동적커리!
        BooleanBuilder builder = new BooleanBuilder();

        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        //특정나이이상
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        //특정 나이 이하
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }


        //builder이용 동적쿼리 및 한번에 다들고오는 성능 최적화까지!
        //QMemberTeamDto에 담는다.
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.Id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)  //team의 데이터를 다가져오기위해서 leftJoin
                .where(builder)
                .fetch();
    }


    public List<MemberTeamDto> searchByWhereParam(MemberSearchCondition condition) {


        //builder이용 동적쿼리 및 한번에 다들고오는 성능 최적화까지!
        //QMemberTeamDto에 담는다.

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.Id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)  //team의 데이터를 다가져오기위해서 leftJoin
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                ).fetch();

    }


    public List<Member> searchMember(MemberSearchCondition condition) {


        return queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)  //team의 데이터를 다가져오기위해서 leftJoin
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
                ).fetch();

    }


    //hasText 빈문자열 null체크 까지 다해서 아니면 true반환
    //BooleanExpression 이렇게 하면 and 조합이 가능
    //재사용가능

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


}
