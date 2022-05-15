import axios from 'axios';
import { getToken } from './jWT-Token';

const BASE_URL = process.env.REACT_APP_API_ROOT + '/game';
// const BASE_URL = 'https://k6a203.p.ssafy.io/apiv1/game';


const createRoom = async () => {
  const response = await axios.post(`${BASE_URL}/room`);
  console.log(response);
  return response;
};

const enterRoom = async (code: string) => {
  const response = await axios.patch(`${BASE_URL}/room`, { inviteCode: code })
                          .then((res) => {
                            const value = {
                              status : 200,
                            }
                            return value;
                          })
                          .catch((e) => {
                            const value = {
                              status : 400,
                            }
                            return value;
                          });
  console.log(response);
  return response;
};

const exitRoom = async (code: string | null) => {
  if(code === null) return null;
  const response = await axios.patch(`${BASE_URL}/exit`, { inviteCode: code });
  console.log(response);
  return response;
};

const getUploadImageResult = async (data: FormData) => {
  const response = await axios.post(`${BASE_URL}/upload`, data);
  return response;
}
const getSaveMyFavoriteImageResult = async (data: { userSeq: string | number | null, pictureUrl: string }) => {
  if (data.userSeq !== null) {
    data.userSeq = +data.userSeq;
    const response = await axios.post(`${BASE_URL}/picture`, data);
    return response
  }
  else return null;
}

//게임 초대 
const postInviteFriendAlarm = async (userSeq: any, inviteCode: any, targetUserSeq: any) => {
  const token = getToken();
  const body = {
    inviteCode,
    userSeq,
    targetSeq: targetUserSeq
  };
  if (token !== null) {
    console.log(body)
    const result = await axios.post(`${BASE_URL}/invite`, body, { headers: { Authorization: token } })
      .then((res) => {
        console.log(res)
        return res;
      })
      .catch((err) => {
        console.dir(err);
        return err;
      });
    return result;
  }
  return null;
};

const RoomApi = {
  createRoom,
  enterRoom,
  exitRoom,
  getUploadImageResult,
  postInviteFriendAlarm,
  getSaveMyFavoriteImageResult
};

export default RoomApi;
