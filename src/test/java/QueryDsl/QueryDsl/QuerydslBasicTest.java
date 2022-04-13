package QueryDsl.QueryDsl;

import QueryDsl.QueryDsl.dto.MemberDto;
import QueryDsl.QueryDsl.dto.QMemberDto;
import QueryDsl.QueryDsl.dto.UserDto;
import QueryDsl.QueryDsl.entity.Member;
import QueryDsl.QueryDsl.entity.QMember;
import QueryDsl.QueryDsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import static QueryDsl.QueryDsl.entity.QMember.member;
import static QueryDsl.QueryDsl.entity.QTeam.team;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

//메서드가 종료될 때 자동으로 롤백된다.
// id는 Auto Increment 는 트랜잭션과 별개로 동작한다.
@Transactional
@SpringBootTest
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    //filed 레벨 가능
    JPAQueryFactory queryFactory = new JPAQueryFactory(em);

    @BeforeEach  //테스트 메서드 진행전에 입력할 값이 있으면 여기에 셋팅
    public void before() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);


    }

    @Test
    public void queryDslTest() {
        //필드로 빼도 괜찮은 동시성 문제 x
        queryFactory = new JPAQueryFactory(em);

        //SELFJOIN 시 이렇게 생성해서 쓰면됨
        //QMember me=new QMember("A");


        //이런방식 권장
        Member result = queryFactory
                .select(member)  // static이기때문에 앞에 Qmember를 임폴트해줌
                .from(member)
                .where(member.username.eq("member1"))  //파라미터 바인딩
                .fetchOne();

        assertThat(result.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search() {
        queryFactory = new JPAQueryFactory(em);

        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))

                .fetchOne();

        assertThat(result.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search1() {
        queryFactory = new JPAQueryFactory(em);

        //2번째방법 and 없이 ,로  동적쿼리시 이용
//        Member result = queryFactory
//                .selectFrom(member)
//                .where(member.username.eq("member1")
//                        ,member.age.eq(10))
//                .fetchOne();
//
//        assertThat(result.getUsername()).isEqualTo("member1");
//
//
        List<Member> fetch = queryFactory.selectFrom(member).fetch();
//        long l = queryFactory.selectFrom(member).fetchCount();
        fetch.stream().forEach(member1 -> System.out.println(member1.getUsername()));

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (DESC)
     * 2. 회원 이름  올림차순 (ASC)
     * 단 2에서 이름이 없는경우 제일 마지막 (nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member5");
        assertThat(result.get(2).getUsername()).isNull();  // null 여부 확인


    }

    @Test
    public void page() {
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        em.persist(new Member("member7", 100));

        queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.username.asc())
                .offset(0) // 두번째부터
                .limit(3)  // 2개
                .fetch();
        assertThat(result.get(0).getUsername()).isEqualTo("member5");

    }

    @Test
    public void page1() {

        queryFactory = new JPAQueryFactory(em);
        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 두번째부터
                .limit(2)  // 2개
                .fetchResults();
//페이징 커리가 복잡할경우 count 커리는 따로 뽑아서 한다. <실무에서>  성능문제떄문에
        assertThat(memberQueryResults.getTotal()).isEqualTo(4); //총갯수
        assertThat(memberQueryResults.getResults().size()).isEqualTo(2); //결과의 갯수

    }

    @Test //집합함수
    public void aggregation() {

        queryFactory = new JPAQueryFactory(em);
        List<Tuple> fetch = queryFactory
                .select(
                        member.count(),
                        member.age.max(),
                        member.age.avg(),
                        member.age.min(),
                        member.age.sum()
                ).from(member).fetch();

//페이징 커리가 복잡할경우 count 커리는 따로 뽑아서 한다. <실무에서>  성능문제떄문에
        Tuple tuple = fetch.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4); //총갯수
        assertThat(tuple.get(member.age.max())).isEqualTo(40); //결과의 갯수
        assertThat(tuple.get(member.age.min())).isEqualTo(10); //결과의 갯수

    }

    /**
     * 목표 : 팀의 이름과 각 팀의 평균 나이를 구하라
     */

    @Test //집합함수
    public void group() {

        queryFactory = new JPAQueryFactory(em);
        List<Tuple> fetch = queryFactory
                .select(team.name, member.age)
                .from(member)
                .join(member.team, team)
                .groupBy(team.name, member.age)
                .having(member.age.gt(15))
                .fetch();

        //그룹바이를 했기때문에 2개 나옴
        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);
        assertThat(teamA.get(member.age)).isEqualTo(35); //총갯수
        assertThat(teamB.get(member.age)).isEqualTo(40); //결과의 갯수


    }

    @Test
    public void join() {
        queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)  //조인
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")//해당 프로펄티를 꺼내서
                .containsExactly("member1", "member2"); // 안에 있는 확인

    }

    /**
     * 예) 회원과 팀을 조인  , 팀 이름이 teamA인 팀만 조인 , 회원은 모두 조회
     * JPQL : select m , t from Member m left join m.team t on t.name= 'teamA'
     * tuple = [Member(id=3, username=member1, age=10), Team(name=teamA)]
     * tuple = [Member(id=4, username=member2, age=35), Team(name=teamA)]
     * tuple = [Member(id=5, username=member3, age=10), null]
     * tuple = [Member(id=6, username=member4, age=40), null]
     */

    @Test
    public void join_on_filtering() {

        queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory  //select가 여러가지라서 Tuple로 나옴
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                //.where(team.name.eq("teamA")) //일반 join시 사용
                .on(team.name.eq("teamA")).fetch(); //leftjoin 시 on을 사용하여 필터링

        //iter
        for (Tuple tuple : result) {
            //souv
            System.out.println("tuple = " + tuple);

        }

    }


    /**
     * 회원의 이름이 팀 이름과 같은 대상 외부조인
     */
    @Test
    public void join_on_no_relation() {

        queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory  //select가 여러가지라서 Tuple로 나옴
                .select(member, team)
                .from(member)
                .leftJoin(team) //연관관계없는맵핑
                .on(member.username.eq(team.name)).fetch(); //조건건

        //iter
        for (Tuple tuple : result) {
            //souv
            System.out.println("tuple = " + tuple);

        }

    }


    //엔티티 매니저 팩토리 가져올수있는 어노테이션
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test // 기본설정은  @ManyToOne(fetch = FetchType.LAZY)  맴버만 검색해온다. 성능 최적화위해서
    public void fetchJoinNo() {
        //fetch 테스트시 다 비워주자
        em.flush();
        em.clear();

        queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory.selectFrom(QMember.member).where(QMember.member.username.eq("member1")).fetchOne();

        //초기화 여부 확인
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        //패치조인을 적용하지 않았기때문에 team을 검색x 그래서 false 예상
        assertThat(loaded).as("패치 조인 미적용").isFalse();

    }


    @Test // 패치 조인 사용 , 검색시 team까지 한번에 가져오게
    public void fetchJoinUse() {
        //fetch 테스트시 다 비워주자
        em.flush();
        em.clear();

        queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .join(member.team, team)
                .fetchJoin() // team까지 한번에 가져온다
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        //초기화 여부 확인
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        //패치조인을 적용 team도 가져오기때문에 그래서 true 예상
        assertThat(loaded).as("패치 조인 적용").isTrue();

    }

    /**
     * 나이가 가장 많은 회원 조회 max
     * 서브커리 사용  JPAExpressions  이용
     * 서브커리쪽에 쓰는 member가 필요하기때문에 Qmember 1개 더 생성
     */
    @Test
    public void subQueryEqAgeMax() {

        QMember memberSub = new QMember("memberSub");

        queryFactory = new JPAQueryFactory(em);

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();

        //이름
        assertThat(result.get(0).getUsername()).isEqualTo("member4");
        //아래처럼 테스트가 훨씬 좋다!
        assertThat(result).extracting("age").containsExactly(40);
    }


    /**
     * 나이가 평균 이상인 회원  goe >=
     * 서브커리 사용  JPAExpressions  이용
     * 서브커리쪽에 쓰는 member가 필요하기때문에 Qmember 1개 더 생성
     */
    @Test
    public void subQueryGoeAgeAvg() {

        QMember memberSub = new QMember("memberSub");

        queryFactory = new JPAQueryFactory(em);

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                )).fetch();


        //평균 나익 25세이상
        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * 나이가 25세 이상인 회원들을
     * in절 사용
     */

    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        queryFactory = new JPAQueryFactory(em);

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.goe(25))
                )).fetch();


        //평균 나이 25세이상
        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * select 절에 subQuery 사용하여 나이 평균 칼럼을 가져옴
     * JPAExpressions 서브쿼리 사용
     * from절의 서브커리 in line view는 지원 x
     */

    @Test
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        queryFactory = new JPAQueryFactory(em);

        List<Tuple> fetch = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            //sout
            System.out.println("tuple = " + tuple);

        }

    }

    /**
     * case 예문
     * 1.basicCase 기본 case when otherwise
     * 2.complexCase between절 있는 case문 new CaseBuilder()사용
     */

    @Test
    public void basicCase() {

        queryFactory = new JPAQueryFactory(em);

        List<String> result =
                queryFactory.select(member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("나이 존내 많네"))
                        .from(member).fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void complexCase() {
        queryFactory = new JPAQueryFactory(em);

        List<String> result = queryFactory.select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~23살")
                        .otherwise("기타"))
                .from(member).fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    //사용자 예문
//    @Test
//    void simpleCaseByOrderBy() {
//
//        NumberExpression<Integer> roleRankPath = new CaseBuilder()
//                .when(member.rolename.eq("ROLE_MASTER")).then(1)
//                .when(member.rolename.eq("ROLE_ADMIN")).then(2)
//                .otherwise(3);
//
//        List<Tuple> fetch = queryFactory
//                .select(
//                        member.username,
//                        member.rolename,
//                        roleRankPath.as("roleRank")
//                )
//                .from(member)
//                .orderBy(roleRankPath.desc())
//                .fetch();
//
//        for (Tuple tuple : fetch) {
//            System.out.println("tuple.get(member.username) = " + tuple.get(member.username));
//            System.out.println("tuple.get(member.rolename) = " + tuple.get(member.rolename));
//            System.out.println("tuple.get(roleRankPath) = " + tuple.get(roleRankPath));
//        }
//        System.out.println("result = " + fetch);

    /**
     * 1.constant
     * 상수 , 문자열 필요시
     * Expressions.constant("문자열") 사용
     * [member4, 추가된 글]
     * <p>
     * 2.concat 문자 더하기
     * concat() 메서드이용
     * username _ age(int 타입)
     * stringValue() 이용  String타입변환 < ENUM > 처리시 자주사용
     */
    @Test
    public void constant() {
        queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("추가된 글"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {
        queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * projection 타입이 한개란?
     * List<Member> , List<String>
     * <p>
     * 두개일때는?
     * Tuple 이용
     * 해당 이름 꺼내기
     * tuple.get(member.username)
     * Tuple은 repository 계층에서만 사용 던질때는 DTO로
     */
    @Test
    public void tupleProjection() {
        queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple.get(member.username).toString() = " + tuple.get(member.username));
            System.out.println(tuple.get(member.age));
        }
    }

    /**
     * QueryDsl이용 바로 Dto에 넣어주기
     * Projections 이용  //기본생성자 필요 !
     * 1.setter  - > Projections.bean(MemberDto.class, member.username, member.age)
     * 2.생성자   -  >  Projections.constructor <타입 주의>
     * 3.filed   -  > Projections.fields
     * 4.다른 DTO에 넣어줄때
     * 필드명이 다르다면 as를 이용 ! member.username.as("name")
     * 아니면 userDto = UserDto(name=null, age=10)
     */

    @Test
    public void findDtoBySetter() {
        queryFactory = new JPAQueryFactory(em);
        List<MemberDto> resultList = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);

        }
    }

    @Test
    public void findDtoByField() {
        queryFactory = new JPAQueryFactory(em);
        List<MemberDto> resultList = queryFactory
                .select(Projections.fields(MemberDto.class
                        , member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);

        }
    }

    @Test
    public void findDtoByConstructor() {
        queryFactory = new JPAQueryFactory(em);
        List<MemberDto> resultList = queryFactory
                .select(Projections.constructor(MemberDto.class
                        , member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);

        }
    }

    @Test
    public void findUserDto() {
        queryFactory = new JPAQueryFactory(em);
        List<UserDto> resultList = queryFactory
                .select(Projections.fields(UserDto.class
                        , member.username.as("name")
                        , member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : resultList) {
            System.out.println("userDto = " + userDto);

        }
    }

    /**
     * sub커리 이용 Dto 넣어주기  < 필드명이 다를때 >
     * ExpressionUtils.as(JPAExpressions.select(membersub.age.max())
     * .from(membersub),"age")))
     */
    @Test
    public void findSubQuery() {
        queryFactory = new JPAQueryFactory(em);
        QMember membersub = new QMember("memberSub");
        List<UserDto> resultList =
                queryFactory
                        .select(Projections.fields(UserDto.class
                                , member.username.as("name")
                                , ExpressionUtils.as(JPAExpressions.select(membersub.age.max())
                                        .from(membersub), "age")))
                        .from(member)
                        .fetch();

        for (UserDto userDto : resultList) {
            System.out.println("userDto = " + userDto);

        }
    }

    /**
     * DTO도 Q파일로 생성
     * other의 compileQuryDsl 해줘야 한다
     *
     * @QueryProjection 생성자 위에
     * <p>
     * MemberDto로 바로 반환
     * 컴파일시 바로 잡을수 있다.! 위쪽 3개는 런타임시 잡을수있음
     * 다만 Dto가 queryDsl에 의존적으로 변화기떄문에 순수하지 않아짐.
     */

    @Test
    public void findDtoByQueryProjection() {
        queryFactory = new JPAQueryFactory(em);
        List<MemberDto> resultList = queryFactory
                .select(new QMemberDto(member.username.as("ssss"), member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);

        }
    }

    /**
     * 동적커리
     * 1.BooleanBuilder
     * 2.Where 다중 파라미터 사용 <김영한님이 실무에서 진짜 좋아하는 부분>
     * 가독성 좋음 , 조립도 가능! 다른 커리에서 재활용도 가능! null 체크 주의해서 처리!
     */

    //1.BooleanBuilder
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);


        assertThat(result.size()).isEqualTo(1);
    }

    //조건
    private List<Member> searchMember1(String usernameCond, Integer ageParamCond) {

        //member.username.eq(usernameCond) Builder안에 초기값으로 넣어줄수도 있다.
        BooleanBuilder builder = new BooleanBuilder();

        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageParamCond != null) {
            builder.and(member.age.eq(ageParamCond));
        }
        queryFactory = new JPAQueryFactory(em);

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();

    }

    //2.Where 다중 파라미터
    //usernameEq ,ageEq 메서드 만들어서 사용
    //where에 null이 오면 무시해버린다
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);


        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        queryFactory = new JPAQueryFactory(em);
        return queryFactory
                .selectFrom(member)
                //.where(usernameEq(usernameCond),ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();

    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;

    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {

        // null일 경우 메소드 체이닝 문제!

        return usernameCond != null ? usernameEq(usernameCond).and(ageEq(ageCond)) : ageEq(ageCond);
    }

    /**
     * 벌크연산 (한번에 수정)
     * 중요 : 벌크연산은 영속성 컨테스트의 상태를 무시하고 바로 DB에 커리문을 날린다.
     * 반대로 영속성 컨텍스트의 해당 id의 엔티티가 있는경우 db에서 가져온 것을 버린다. (영속성 컨테스트 우선)
     * 그래서 영속성 컨테스트와 동기화를 시켜줄 필요가 있다!!
     *
     * @Commit 여기서만 commit
     * 테스트는 자동 rollback시킨다.
     */
    @Test
    public void bulkUpdate() {

        queryFactory = new JPAQueryFactory(em);
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        /**
         *   벌크연산시 영속성 컨텍스트를 비우고 db와 동기화시킨다 !
         *   스프링data jpa의 @modifyed
         */
        em.flush();
        em.clear();


        List<Member> fetch = queryFactory.selectFrom(member).fetch();
        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }

    }

    /**
     * add(1) 더하기  / add(-1) 마이너스
     * multiply(2) 곱하기
     */
    @Test
    public void bulkAdd() {
        queryFactory = new JPAQueryFactory(em);
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(10))
                .where(member.username.eq("member1"))
                .execute();
        //동기화
        em.clear();
        em.flush();

        Member member = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        assertThat(member.getAge()).isEqualTo(20);
    }

    /**
     * 벌크 삭제
     */
    @Test
    public void bulkDelete() {
        queryFactory = new JPAQueryFactory(em);
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * SQL 함수 사용 , Expressions.stringTemplate()
     * ex) replace
     * select replace(member0_.username, 'member', 'M') as col_0_0_ from member member0_;
     */

    @Test
    public void sqlFunction() {
        queryFactory = new JPAQueryFactory(em);
        List<String> fetch = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace',{0},{1},{2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);

        }
    }

    /**
     * lower() 써서 lower함수 대체
     * upper() 대문자
     */
    @Test
    public void sqlFunction2() {
        queryFactory = new JPAQueryFactory(em);
        List<String> fetch1 = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
//                        Expressions
//                                .stringTemplate("function('lower',{0})", member.username)))
                        member.username.lower()))
                .fetch();


        for (String s : fetch1) {
            System.out.println("s = " + s);

        }
    }


}
