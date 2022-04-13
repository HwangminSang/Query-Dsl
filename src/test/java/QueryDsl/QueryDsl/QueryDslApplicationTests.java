package QueryDsl.QueryDsl;

import QueryDsl.QueryDsl.entity.EntityTest;
import QueryDsl.QueryDsl.entity.Member;
import QueryDsl.QueryDsl.entity.QEntityTest;
import QueryDsl.QueryDsl.entity.Team;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static QueryDsl.QueryDsl.entity.QEntityTest.*;


@Transactional
@SpringBootTest
@Commit
class QueryDslApplicationTests {

    @Autowired
    EntityManager em;


    @Test
    void contextLoads() {
        EntityTest t = new EntityTest();
        em.persist(t);
        //querydsl 사용

        JPAQueryFactory query = new JPAQueryFactory(em);
        QEntityTest Qhello = entityTest;//엘리어스 넣어줌

        EntityTest result = query
                .selectFrom(Qhello)
                .fetchOne();
        Assertions.assertThat(result).isEqualTo(t);
        Assertions.assertThat(result.getId()).isEqualTo(t.getId());
    }


    @Test
    void tset() {


    }
}
