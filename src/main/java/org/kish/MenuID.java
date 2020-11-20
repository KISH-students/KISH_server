package org.kish;

public enum MenuID {
    /** 초등 공지사항 메뉴 ID*/
    ELEMENTARY_NOTICE("66", "초등 공지사항"),
    /** 초등 교육과정 메뉴 ID*/
    ELEMENTARY_CURRICULUM("32", "초등 교육과정"),
    /** 초등 가정통신문 메뉴 ID*/
    ELEMENTARY_PARENTS_NOTICES("31", "초등 가정통신문"),
    /** 초등 방과후학교 메뉴 ID*/
    ELEMENTARY_AFTER_SCHOOL("30", "초등 방과후학교"),
    /** 초등 외국어 자료실 메뉴 ID*/
    ELEMENTARY_FOREIGN_LANG_ARCHIVES("29", "초등 외국어 자료실"),
    /** 초등 서식 자료실 메뉴 ID*/
    ELEMENTARY_FORMATTING("28", "초등 서식 자료실"),

    /** 초등 1학년 학습방 메뉴 ID*/
    ELEMENTARY_GRADE_FIRST("86", "초등 1학년 학습방"),
    /** 초등 2학년 학습방 메뉴 ID*/
    ELEMENTARY_GRADE_SECOND("87", "초등 2학년 학습방"),
    /** 초등 3학년 학습방 메뉴 ID*/
    ELEMENTARY_GRADE_THIRD("88", "초등 3학년 학습방"),
    /** 초등 4학년 학습방 메뉴 ID*/
    ELEMENTARY_GRADE_FOURTH("89", "초등 4학년 학습방"),
    /** 초등 5학년 학습방 메뉴 ID*/
    ELEMENTARY_GRADE_FIFTH("90", "초등 5학년 학습방"),
    /** 초등 6학년 학습방 메뉴 ID*/
    ELEMENTARY_GRADE_SIXTH("91", "초등 6학년 학습방"),


    /** 중고등 공지사항 메뉴 ID*/
    HIGH_NOTICE("67", "중고등 공지사항"),
    /** 중고등 교육과정 메뉴 ID*/
    HIGH_CURRICULUM("40", "중고등 교육과정"),
    /** 중고등 가정통신문 메뉴 ID*/
    HIGH_PARENTS_NOTICES("38", "중고등 가정통신문"),
    /** 중고등 방과후학교 메뉴 ID*/
    HIGH_AFTER_SCHOOL("37", "중고등 방과후학교"),
    /** 중고등 진로교육 메뉴 ID*/
    HIGH_CAREER_EDUCATION("77", "중고등 진로교육"),
    /** 중고등 특례입시 메뉴 ID*/
    HIGH_SPECIAL("78", "중고등 특례입시"),
    /** 중고등 드림레터 메뉴 ID*/
    HIGH_DREAM_LETTER("79", "중고등 드림레터"),
    /** 중고등 서식 자료실 메뉴 ID*/
    HIGH_FORMATTING("34", "중고등 서식 자료실"),

    /** 중고등 7학년 학습방 메뉴 ID*/
    HIGH_GRADE_SEVENTH("92", "중고등 7학년 학습방"),
    /** 중고등 8학년 학습방 메뉴 ID*/
    HIGH_GRADE_EIGHTH("93", "중고등 8학년 학습방"),
    /** 중고등 9학년 학습방 메뉴 ID*/
    HIGH_GRADE_NINTH("94", "중고등 9학년 학습방"),
    /** 중고등 10학년 학습방 메뉴 ID*/
    HIGH_GRADE_TENTH("95", "중고등 10학년 학습방"),
    /** 중고등 11학년 학습방 메뉴 ID*/
    HIGH_GRADE_ELEVENTH("96", "중고등 11학년 학습방"),
    /** 중고등 12학년 학습방 메뉴 ID*/
    HIGH_GRADE_TWELFTH("97", "중고등 12학년 학습방"),

    /** 주말한글학교 공지사항 메뉴 ID*/
    HANGUL_NOTICE("55", "주말한글학교 공지사항"),
    /** 주말한글학교 서식자료실 메뉴 ID*/
    HANGUL_FORMATTING("52", "주말한글학교 서식자료실"),

    /** 행정실 메뉴 ID*/
    ADMINISTRATIVE("49", "행정실"),
    /** 스쿨버스 메뉴 ID*/
    SCHOOL_BUS("65", "스쿨버스"),
    /** 도서실안내 메뉴 ID*/
    LIBRARY_INFO("48", "도서실안내"),
    /** 급식안내 메뉴 ID*/
    LUNCH_INFO("74", "급식안내"),
    /** 이사회 메뉴 ID*/
    BOARD_OF_DIRECTORS("45", "이사회"),
    /** 학부모회 메뉴 ID*/
    PARENTS_ASSOCIATION("76", "학부모회"),
    /** 행정실 메뉴 ID*/
    COMMITTEE("62", "행정실"),
    /** 발전기금 메뉴 ID*/
    FUND("43", "학교발전기금"),
    /** 서식자료실 메뉴 ID*/
    FORMATTING("42", "서식자료실"),

    SCHOOL_ALBUM("16", "학교 앨범"),
    SCHOOL_VIDEO_ALBUM("71", "학교 동영상 앨범"),
    /** 보도자료 메뉴 ID */
    SCHOOL_PRESS_RELEASE("72", "보도자료"),

    /** 이전 공지사항 메뉴 ID */
    PREVIOUS_NOTICE("69", "이전 공지사항"),
    /** 이전 서식자료실 메뉴 ID */
    PREVIOUS_FORMATTING("81", "이전 서식자료실");

    public String id;
    public String name;
    
    MenuID(String menuId, String name){
        this.id = menuId;
        this.name = name;
    }
}
