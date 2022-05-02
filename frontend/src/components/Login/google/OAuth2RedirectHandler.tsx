import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { saveToken } from '../../../common/api/jWT-Token';
import UserApi from '../../../common/api/UserApi';

function OAuth2RedirectHandler() {
  //이렇게 가져오는 걸 햇는데!
  // 콘솔에 안찍혀서!
  const code = new URL(window.location.href).searchParams.get('code');
  const navigate = useNavigate();
  const { getGoogleLoginResult } = UserApi;

  // api 통신
  //백으로 code 넘기고 토큰 저장하고, api 통신 연결하고 로그인 이후의 화면으로 보내면 된다. ex, main
  const apiResult = async () => {
    console.log("??????? 왜?? 여기가 구글");
    const result = await getGoogleLoginResult(code as string);

    //넘기고 나는 토큰 저장,, 끝! 헤헷,,,
    // 프론트는 딱히.. 어렵지가 않앙,,항항,,,
    saveToken(result.data.token);
    window.localStorage.setItem('userSeq', result.data.userSeq);
    navigate('/');
    return result;
  };

  useEffect(() => {
    const result = apiResult();
  }, []);
  return <>{code}</>;
}

export default OAuth2RedirectHandler;