import React, { useEffect, useState } from 'react';
import styles from './style/Content.module.scss';
import { ReactComponent as Users } from '../../assets/users.svg';
import { ReactComponent as Bell } from '../../assets/bell-ring.svg';
import { useNavigate } from 'react-router-dom';
import AOS from 'aos';
import { debounce } from 'lodash';

type MyProps = {
  type: number;
};

function ContentFirst({ type }: MyProps) {
  const [screen, setScreen] = useState<number>(window.innerWidth);
  const [checkFlag, setCheck] = useState<boolean>(false);

  const handleResize = debounce(() => {
    AOS.refreshHard();
    setScreen(window.innerWidth);
    // checkSize();
  }, 1000);

  const checkSize = () => {
    if (screen >= 800) {
      if (checkFlag) {
        setCheck(false);
      }
    } else {
      if (!checkFlag) {
        setCheck(true);
      }
    }
  };

  const renderElement = () => {
    if (type === 0) {
      if (checkFlag) {
        return (
          <div className={styles.content}>
            <div data-aos="fade-left" data-aos-duration="500" className={styles.container}>
              <div className={styles.card}>
                <p>Arcade란?</p>
                <span>사람들과 간단한 게임을 즐길 수 있는 웹 기반 게임 서비스 입니다.</span>
              </div>
            </div>
            <div data-aos="fade-right" data-aos-duration="500" className={styles.container}>
              <div className={styles.card}>
              <p>Arcade의 Game</p>
                <span>'캐치마인드' , '몸으로 말해요', '너의 목소리가 들려' 가 있습니다.</span>
              </div>
            </div>
          </div>
        );
      }
      return (
        <div className={styles.content}>
          <div
            data-aos="fade-right"
            data-aos-offset="200"
            // {...window.innerWidth}
            data-aos-duration="500"
            className={styles.container}
          >
            <div className={styles.card}>
              <p>Arcade란?</p>
              <span>사람들과 간단한 게임을 즐길 수 있는 웹 기반 게임 서비스 입니다.</span>
            </div>
          </div>
          <div
            data-aos="fade-right"
            data-aos-offset="200"
            // {...window.innerWidth}
            data-aos-duration="500"
            className={styles.container}
          >
            <div className={styles.card}>
              <p>Arcade의 Game</p>
                <span>'캐치마인드' , '몸으로 말해요', '너의 목소리가 들려' 가 있습니다.</span>
            </div>
          </div>
        </div>
      );
    } else if (type === 1) {
      if (checkFlag) {
        return (
          <div className={styles.content}>
            <div data-aos="fade-left" data-aos-duration="500" className={styles.container}>
              <div className={styles.card}>
                <p>이용방법</p>
                <span>회원가입 없이 닉네임을 설정하고 방을 만들 수 있어요! 입장코드를 통해 방에 참여 해보세요.</span>
              </div>
            </div>
            <div data-aos="fade-right" data-aos-duration="500" className={styles.container}>
              <div className={styles.card}>
                <p>GameRoom</p>
                  <span>한 게임룸에는 최대 6명이서 즐길 수 있어요! 로그인한 회원은 회원끼리 친구 추가를 할 수 있습니다.</span>
              </div>
            </div>
          </div>
        );
      }
      return (
        <div className={styles.content}>
          <div
            data-aos="fade-left"
            data-aos-offset="300"
            // {...window.innerWidth}
            data-aos-duration="500"
            className={styles.container}
          >
            <div className={styles.card}>
              <p>이용방법</p>
                <span>회원가입 없이 닉네임을 설정하고 방을 만들 수 있어요! 입장코드를 통해 방에 참여 해보세요.</span>
            </div>
          </div>
          <div
            data-aos="fade-left"
            data-aos-offset="300"
            // {...window.innerWidth}
            data-aos-duration="500"
            className={styles.container}
          >
            <div className={styles.card}>
            <p>GameRoom</p>
              <span>한 게임룸에는 최대 6명이서 즐길 수 있어요! 로그인한 회원은 회원끼리 친구 추가를 할 수 있습니다.</span>
            </div>
          </div>
        </div>
      );
    }
  };

  useEffect(() => {
    checkSize();
  }, [screen]);

  useEffect(() => {
    window.addEventListener('resize', handleResize);
    AOS.init();
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);
  return <>{renderElement()}</>;
}

export default ContentFirst;
