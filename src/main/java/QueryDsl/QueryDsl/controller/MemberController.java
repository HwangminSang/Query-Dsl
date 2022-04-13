package QueryDsl.QueryDsl.controller;


import QueryDsl.QueryDsl.dto.MemberSearchCondition;
import QueryDsl.QueryDsl.dto.MemberTeamDto;
import QueryDsl.QueryDsl.repository.MemberJpaRepository;
import QueryDsl.QueryDsl.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/jpa")

public class MemberController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {

        return memberJpaRepository.searchByWhereParam(condition);
    }

    /**
     * page 리턴
     * 컨트롤러가 알아서  Pageable에 값을 binding 해준다
     * page=1&size=5   기본은 0부터 . 5개씩
     */

    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {

        return memberRepository.searchPageSimple(condition, pageable);
    }


    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition, Pageable pageable) {

        return memberRepository.searchPageCmplex(condition, pageable);
    }


}
