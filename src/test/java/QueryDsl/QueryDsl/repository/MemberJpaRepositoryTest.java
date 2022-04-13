package QueryDsl.QueryDsl.repository;

import QueryDsl.QueryDsl.dto.MemberSearchCondition;
import QueryDsl.QueryDsl.dto.MemberTeamDto;
import QueryDsl.QueryDsl.entity.Member;
import QueryDsl.QueryDsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        //주소값 비교 <영속성 컨테스트의 맴버와 들어간 맴버 비교>
        assertThat(findMember).isEqualTo(member);


        //해당 맴버가 list에 있는지
        List<Member> memberList = memberJpaRepository.findAll();

        //테스트 맴버확인
        assertThat(memberList).containsExactly(member);


        List<Member> member1 = memberJpaRepository.findByUsername("member1");


        assertThat(member1).containsExactly(member);


    }

    @Test
    public void basicQueryDslTest() {

        Member member = new Member("member1", 10);

        memberJpaRepository.save(member);

        List<Member> member1 = memberJpaRepository.findByUsername_Querydsl("member1");

        assertThat(member1).containsExactly(member);

        List<Member> all_querydsl = memberJpaRepository.findAll_Querydsl();
        assertThat(all_querydsl).containsExactly(member);
    }


    /**
     * 성능 최적화 및 동적커리 QueryDsl이용
     * 엔티티를 Dto까지!
     */
    @Test
    public void searchTest() {
        //등록
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

        /**
         *  검색 :teamB 소속이고 나이가 35세 ~ 40세  사이의 맴버
         *  결과 :username이 member4
         *  아래 조건이 다 해당하지 않으면 전체 회원을 다 가져온다.!!!  그래서 limit가 있는게 좋다. 페이징커리도 같이 들어가면 좋다.
         *  엔티티가 아닌 DTO로 조회하시면 지연로딩을 사용할 수 없기 때문에 N+1 자체가 발생하지 않습니다.
         */
        MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
        memberSearchCondition.setAgeGoe(35);
        memberSearchCondition.setAgeLoe(40);
        memberSearchCondition.setTeamName("teamB");

        //검색
        List<Member> result = memberJpaRepository.searchMember(memberSearchCondition);

        //결과검증
        //속성값 가져와서 이름 체크
        assertThat(result).extracting("username").containsExactly("member4");
        assertThat(result).extracting("username").containsExactly("member4");
    }
}