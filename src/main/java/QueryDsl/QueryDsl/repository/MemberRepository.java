package QueryDsl.QueryDsl.repository;

import QueryDsl.QueryDsl.entity.Member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;


/**
 * MemberRepositoryCustom 우리가 만든 repository로 상속받는다 그러면 MemberRepositoryCustom에 있는 것을 사용가능
 */
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {

    List<Member> findByUsername(String username);

}
