package QueryDsl.QueryDsl.controller;


import QueryDsl.QueryDsl.entity.Member;
import QueryDsl.QueryDsl.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

//테스트용 x
// The following profiles are active: local
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    /**
     * @Transactional
     * @PostConstruct 같이 사용 x 분리 필요 !
     */

    @PostConstruct
    public void init() {

        initMemberService.init();
    }

    @Component
    static class InitMemberService {
        //EntityManager
        @PersistenceContext
        private EntityManager em;


        //데이터 초기화  , 스프링컨테이너가 올라올때 데이터를 넣는다!
        @Transactional
        public void init() {

            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i <= 100; i++) {

                Team selectedTeam = i % 2 == 0 ? teamA : teamB;

                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }
    }

}
