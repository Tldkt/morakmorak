package com.morakmorak.morak_back_end.controller.user_controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morakmorak.morak_back_end.config.SecurityTestConfig;
import com.morakmorak.morak_back_end.controller.ExceptionController;
import com.morakmorak.morak_back_end.controller.UserController;
import com.morakmorak.morak_back_end.dto.UserDto;
import com.morakmorak.morak_back_end.exception.BusinessLogicException;
import com.morakmorak.morak_back_end.exception.ErrorCode;
import com.morakmorak.morak_back_end.mapper.UserMapper;
import com.morakmorak.morak_back_end.security.resolver.JwtArgumentResolver;
import com.morakmorak.morak_back_end.service.auth_user_service.PointService;
import com.morakmorak.morak_back_end.service.auth_user_service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.morakmorak.morak_back_end.config.ApiDocumentUtils.getDocumentRequest;
import static com.morakmorak.morak_back_end.config.ApiDocumentUtils.getDocumentResponse;
import static com.morakmorak.morak_back_end.security.util.SecurityConstants.NICKNAME;
import static com.morakmorak.morak_back_end.util.SecurityTestConstants.JWT_HEADER;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@Import(SecurityTestConfig.class)
@WebMvcTest({UserController.class, ExceptionController.class, UserMapper.class})
@MockBean(JpaMetamodelMappingContext.class)
//@AutoConfigureRestDocs(uriScheme = "https", uriHost = "docs.api.com")
public class UserPointTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    PointService pointService;
    @MockBean
    UserService userService;
    @MockBean
    UserMapper userMapper;

    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    JwtArgumentResolver jwtArgumentResolver;


    @Test
    @DisplayName("유저가 존재하지 않는 경우 포인트 조회 요청 시 404 반환")
    void getUserPoint_failed_1() throws Exception {
        //given
        Long INVALID_USERID = 1L;
        String INVALID_USER_ACCESS_TOKEN = "Invalid access token";

        BDDMockito.given(userService.findVerifiedUserById(INVALID_USERID)).willThrow(new BusinessLogicException(ErrorCode.USER_NOT_FOUND));
        BDDMockito.given(pointService.getRemainingPoint(INVALID_USERID)).willThrow(new BusinessLogicException(ErrorCode.USER_NOT_FOUND));
//        BDDMockito.given(userMapper.toResponsePoint(User.builder().build())).willReturn();
        //when
        ResultActions result = mockMvc.perform(get("/users/points", INVALID_USERID)
                .header(JWT_HEADER, INVALID_USER_ACCESS_TOKEN)
        );
        //then
        result.andExpect(status().isNotFound())
                .andDo(document(
                        "게시글을_등록할때_존재하지_않는_태그를_작성하려할떄_실패_404",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description("존재하지 않는 유저의 access token")
                        )
));
    }

    @Test
    @DisplayName("유효한 포인트 조회 요청인 경우 200, responsePoint 반환")
    void getUserPoint_succes_1() throws Exception {
        //given
        Long VALID_USERID = 1L;
        UserDto.ResponsePoint response = UserDto.ResponsePoint.builder().point(10).userId(VALID_USERID).nickname(NICKNAME).build();
        String VALID_USER_ACCESS_TOKEN = "Valid access token";

        BDDMockito.given(pointService.getRemainingPoint(any())).willReturn(response);
        //when
        ResultActions result = mockMvc.perform(get("/users/points", VALID_USERID)
                .header(JWT_HEADER, VALID_USER_ACCESS_TOKEN)
        );
        //then
        result.andExpect(status().isOk())
                .andDo(
                        document("포인트 조회 요청 성공_200",
                                getDocumentRequest(),
                                getDocumentResponse(),
                                requestHeaders(
                                        headerWithName(JWT_HEADER).description("액세스 토큰")
                                ),
                                responseFields(List.of(
                                        fieldWithPath("userId").description("조회하는 유저 아이디"),
                                        fieldWithPath("nickname").description("조회하는 유저 닉네임"),
                                        fieldWithPath("point").description("조회하는 유저 잔여포인트"))
                                )
                        )
                );

    }
}