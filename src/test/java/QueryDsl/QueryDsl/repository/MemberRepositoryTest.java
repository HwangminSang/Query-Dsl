package QueryDsl.QueryDsl.repository;

import QueryDsl.QueryDsl.dto.MemberSearchCondition;
import QueryDsl.QueryDsl.dto.MemberTeamDto;
import QueryDsl.QueryDsl.entity.Member;
import QueryDsl.QueryDsl.entity.QMember;
import QueryDsl.QueryDsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static QueryDsl.QueryDsl.entity.QMember.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;


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
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        //주소값 비교 <영속성 컨테스트의 맴버와 들어간 맴버 비교>
        assertThat(findMember).isEqualTo(member);

        //해당 맴버가 list에 있는지
        List<Member> memberList = memberRepository.findAll();
        assertThat(memberList).containsExactly(member);

        List<Member> member1 = memberRepository.findByUsername("member1");

        assertThat(member1).containsExactly(member);


    }

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
        List<MemberTeamDto> result = memberRepository.search(memberSearchCondition);

        //결과검증
        //속성값 가져와서 이름 체크
        assertThat(result).extracting("username").containsExactly("member4");

    }

    /**
     * page 테스트트
     * 첫번째부터 3개만
     * 0번쨰부터 시작함
     */
    @Test
    public void searchSimplePageTest() {
        //조건


        MemberSearchCondition MemberSearchCondition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<MemberTeamDto> result = memberRepository.searchPageSimple(MemberSearchCondition, pageRequest);

        //테스트
        // 갯수 result.getSize()
        // getContent() 내용 List 반환
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");

    }

    /**
     * queryDslPrediacateExecutor 사용
     * findAll 사용 안에 Qmember사용가능
     * <p>
     * 한계 : 조인 x  left join 안된다.
     * : 복잡한 실무환경에서 사용하기에는 한계가 명확하다.
     * : Pageable , Sort 기능도 제공
     * <p>
     * <p>
     * QueryDsl Web 사용 x
     */

    @Test
    public void queryDslPrediacateExecutor() {
        //조건
        QMember member = QMember.member;
        Iterable<Member> memeber2 = memberRepository.findAll(member.age.between(10, 40).and(member.username.eq("memeber2")));
        for (Member member1 : memeber2) {
            System.out.println("member1 = " + member1);
        }
    }


}