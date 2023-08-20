package com.jeremy.thunder

/**
 * Socket Thunder State
 * 기존 Scarlet 라이브러리에선 Event 상태를 체크해서 활성화 상태일 때만 Send를 할 수 있는 구조였기에 CONNECTED 상태 외에 모두 캐시 처리하고 저장하는 것이 필요
* */
sealed interface ThunderState {

    /*
    * 초기 소켓 상태
    * */
    object IDLE : ThunderState

    /*
    * 연결 중인 상태
    * Send 요청 시, Cache 처리
    * */
    object CONNECTING : ThunderState

    /*
    * 연결 된 상태
    * Send 요청 시, Cache 체크 후 데이터 전송
    * */
    object CONNECTED : ThunderState

    /*
    * 연결을 끊고 있는 상태
    * Send 요청 시, 무시 (Cache Clear)
    * */
    object DISCONNECTING : ThunderState

    /*
    * 연결이 끊긴 상태
    * Send 요청 시, 무시 (Cache Clear)
    * */
    object DISCONNECTED : ThunderState

    /*
    * 에러가 난 상태
    * Send 요청 시, Cache 처리
    * */
    data class ERROR(
        val error: ThunderError
    ) : ThunderState
}