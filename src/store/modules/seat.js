import { getSeatStatus, getFloors, getSeatDetail, reserveSeat, cancelReservation,
  checkIn, checkOut, getCurrentReservation, checkTimeout } from '@/api/seats'
import { SEAT_STATUS } from '@/utils/seatUtils'
import { Message } from 'element-ui'

const state = {
  floors: {},
  currentFloor: 1,
  totalFloors: 5,
  activeReservation: null,
  pastReservations: [],
  timeoutChecker: null
}

const mutations = {
  SET_FLOORS(state, floors) {
    state.floors = floors
  },
  SET_CURRENT_FLOOR(state, floorNumber) {
    state.currentFloor = floorNumber
  },
  SET_TOTAL_FLOORS(state, totalFloors) {
    state.totalFloors = totalFloors
  },
  SET_FLOOR_SEATS(state, { floorNumber, seats }) {
    state.floors[floorNumber] = seats
  },
  UPDATE_SEAT(state, { floorNumber, seatNumber, updates }) {
    if (!state.floors[floorNumber]) return

    const seatIndex = state.floors[floorNumber].findIndex(
      seat => seat.seatNumber === seatNumber
    )

    if (seatIndex !== -1) {
      state.floors[floorNumber][seatIndex] = {
        ...state.floors[floorNumber][seatIndex],
        ...updates
      }
    }
  },
  SET_ACTIVE_RESERVATION(state, reservation) {
    state.activeReservation = reservation
    // 保存到本地存储，确保页面刷新后还能获取到预约信息
    if (reservation) {
      localStorage.setItem('activeReservation', JSON.stringify(reservation))
    } else {
      localStorage.removeItem('activeReservation')
    }
  },
  ADD_PAST_RESERVATION(state, reservation) {
    state.pastReservations.unshift(reservation)
  },
  SET_TIMEOUT_CHECKER(state, checker) {
    state.timeoutChecker = checker
  },
  CLEAR_TIMEOUT_CHECKER(state) {
    if (state.timeoutChecker) {
      clearInterval(state.timeoutChecker)
      state.timeoutChecker = null
    }
  }
}

const actions = {
  // 初始化楼层数据
  async initializeFloor({ commit, dispatch }, floorNumber) {
    try {
      // 获取楼层信息
      const floorsResponse = await getFloors()
      if (floorsResponse.code === '0') {
        const floors = floorsResponse.data
        // 设置总楼层数
        commit('SET_TOTAL_FLOORS', floors.length)
        commit('SET_CURRENT_FLOOR', floorNumber || 1)
      }

      // 获取当前活跃预约
      await dispatch('fetchCurrentReservation')

      return true
    } catch (error) {
      console.error('初始化楼层数据失败:', error)
      return false
    }
  },

  // 更改当前楼层
  changeFloor({ commit, dispatch }, floorNumber) {
    commit('SET_CURRENT_FLOOR', floorNumber)
    // 获取该楼层的座位状态
    dispatch('fetchFloorSeats', floorNumber)
  },

  // 获取指定楼层的座位状态
  async fetchFloorSeats({ commit }, floorNumber) {
    try {
      const response = await getSeatStatus(floorNumber)
      console.log('获取到的座位状态:', response)

      // 检查响应结构
      if (response.code === '0' && response.data) {
        // 标准响应格式
        const seats = response.data
        commit('SET_FLOOR_SEATS', {
          floorNumber,
          seats: seats
        })
        console.log('更新楼层座位状态成功(标准格式):', seats)
        return seats
      } else if (Array.isArray(response)) {
        // 数组格式（后端直接返回数组）
        const seats = response
        commit('SET_FLOOR_SEATS', {
          floorNumber,
          seats: seats
        })
        console.log('更新楼层座位状态成功(数组格式):', seats)
        return seats
      } else {
        console.warn('座位数据格式不符合预期:', response)
        return []
      }
    } catch (error) {
      console.error('获取座位状态失败:', error)
      return []
    }
  },

  // 获取座位详情
  async fetchSeatDetail({ commit }, { floorNumber, seatNumber }) {
    try {
      const response = await getSeatDetail(floorNumber, seatNumber)
      if (response.code === '0') {
        commit('UPDATE_SEAT', {
          floorNumber,
          seatNumber,
          updates: response.data
        })
        return response.data
      }
      return null
    } catch (error) {
      console.error('获取座位详情失败:', error)
      return null
    }
  },

  // 预约座位
  async reserveSeat({ commit, dispatch, state }, reservationData) {
    try {
      const response = await reserveSeat(reservationData)
      if (response.code === '0') {
        // 确保预约数据包含所有必需字段
        const activeReservation = {
          ...response.data,
          // 确保包含必要信息
          floorNumber: reservationData.floorNumber || response.data.floorNumber,
          seatNumber: reservationData.seatNumber || response.data.seatNumber,
          status: response.data.status !== undefined ? response.data.status : 1 // 如果后端没有返回状态，则默认为1表示"预约中"
        }

        // 更新活跃预约
        commit('SET_ACTIVE_RESERVATION', activeReservation)

        // 更新该座位状态 - 直接将座位状态设置为"预约中"
        if (state.floors[activeReservation.floorNumber]) {
          commit('UPDATE_SEAT', {
            floorNumber: activeReservation.floorNumber,
            seatNumber: activeReservation.seatNumber,
            updates: { status: 1 } // 使用状态值1表示"预约中"
          })
          console.log('已更新座位状态为"预约中"')
        }

        // 从后端获取最新的座位状态
        await dispatch('fetchSeatDetail', {
          floorNumber: activeReservation.floorNumber,
          seatNumber: activeReservation.seatNumber
        })

        return {
          success: true,
          data: activeReservation
        }
      }

      return {
        success: false,
        message: response.msg || '预约失败'
      }
    } catch (error) {
      console.error('预约座位失败:', error)
      return {
        success: false,
        message: '预约请求出错'
      }
    }
  },

  // 取消预约
  async cancelReservation({ commit, state, dispatch }) {
    if (!state.activeReservation) {
      return {
        success: false,
        message: '没有活跃的预约'
      }
    }

    try {
      const response = await cancelReservation(state.activeReservation.reservationId)
      if (response.code === '0') {
        // 更新座位状态为"可预约"
        if (state.floors[state.activeReservation.floorNumber]) {
          const seatIndex = state.floors[state.activeReservation.floorNumber].findIndex(
            seat => seat.seatNumber === state.activeReservation.seatNumber
          )

          if (seatIndex !== -1) {
            // 直接更新座位状态为"可预约"
            commit('UPDATE_SEAT', {
              floorNumber: state.activeReservation.floorNumber,
              seatNumber: state.activeReservation.seatNumber,
              updates: { status: 0 } // 使用状态值0表示"可预约"
            })
            console.log('已更新座位状态为"可预约"')
          }
        }

        // 添加到历史预约
        commit('ADD_PAST_RESERVATION', {
          ...state.activeReservation,
          status: 4, // 使用数字4表示"已取消"
        })

        // 清除活跃预约
        commit('SET_ACTIVE_RESERVATION', null)

        // 更新该座位状态
        await dispatch('fetchSeatDetail', {
          floorNumber: state.activeReservation.floorNumber,
          seatNumber: state.activeReservation.seatNumber
        })

        return {
          success: true
        }
      }

      return {
        success: false,
        message: response.msg || '取消预约失败'
      }
    } catch (error) {
      console.error('取消预约失败:', error)
      return {
        success: false,
        message: '取消预约请求出错'
      }
    }
  },

  // 签到
  async checkIn({ commit, state, dispatch }) {
    if (!state.activeReservation) {
      return {
        success: false,
        message: '没有活跃的预约'
      }
    }

    try {
      const response = await checkIn(state.activeReservation.reservationId)
      if (response.code === '0') {
        console.log('签到成功，原状态:', state.activeReservation.status)

        // 更新活跃预约状态 - 使用数字2表示"已占用"状态
        const updatedReservation = {
          ...state.activeReservation,
          status: 2,  // 使用数字2表示"已占用"状态
          realStartTime: new Date().toISOString()
        }

        console.log('更新后的预约状态:', updatedReservation.status)
        commit('SET_ACTIVE_RESERVATION', updatedReservation)

        // 立即刷新本地存储以确保状态持久化
        localStorage.setItem('activeReservation', JSON.stringify(updatedReservation))

        // 更新对应座位的状态 - 确保座位状态和预约状态同步
        if (state.floors[updatedReservation.floorNumber]) {
          const seatIndex = state.floors[updatedReservation.floorNumber].findIndex(
            seat => seat.seatNumber === updatedReservation.seatNumber
          )

          if (seatIndex !== -1) {
            // 直接更新座位状态为"已占用"
            commit('UPDATE_SEAT', {
              floorNumber: updatedReservation.floorNumber,
              seatNumber: updatedReservation.seatNumber,
              updates: { status: 2 } // 使用相同的状态值2表示"已占用"
            })
            console.log('已更新座位状态为"已占用"')
          }
        }

        // 更新该座位状态 - 从后端获取最新状态
        await dispatch('fetchSeatDetail', {
          floorNumber: state.activeReservation.floorNumber,
          seatNumber: state.activeReservation.seatNumber
        })

        // 强制重新获取当前预约状态来确认更新
        setTimeout(() => {
          dispatch('fetchCurrentReservation')
        }, 500)

        return {
          success: true
        }
      }

      return {
        success: false,
        message: response.msg || '签到失败'
      }
    } catch (error) {
      console.error('签到失败:', error)
      return {
        success: false,
        message: '签到请求出错'
      }
    }
  },

  // 退座
  async checkOut({ commit, state, dispatch }) {
    if (!state.activeReservation) {
      return {
        success: false,
        message: '没有活跃的预约'
      }
    }

    try {
      const response = await checkOut(state.activeReservation.reservationId)
      if (response.code === '0') {
        // 更新座位状态为"可预约"
        if (state.floors[state.activeReservation.floorNumber]) {
          const seatIndex = state.floors[state.activeReservation.floorNumber].findIndex(
            seat => seat.seatNumber === state.activeReservation.seatNumber
          )

          if (seatIndex !== -1) {
            // 直接更新座位状态为"可预约"
            commit('UPDATE_SEAT', {
              floorNumber: state.activeReservation.floorNumber,
              seatNumber: state.activeReservation.seatNumber,
              updates: { status: 0 } // 使用状态值0表示"可预约"
            })
            console.log('已更新座位状态为"可预约"')
          }
        }

        // 添加到历史预约
        commit('ADD_PAST_RESERVATION', {
          ...state.activeReservation,
          status: 5, // 使用数字5表示"已完成"
          realEndTime: new Date().toISOString()
        })

        // 清除活跃预约
        commit('SET_ACTIVE_RESERVATION', null)

        // 更新该座位状态
        await dispatch('fetchSeatDetail', {
          floorNumber: state.activeReservation.floorNumber,
          seatNumber: state.activeReservation.seatNumber
        })

        return {
          success: true
        }
      }

      return {
        success: false,
        message: response.msg || '退座失败'
      }
    } catch (error) {
      console.error('退座失败:', error)
      return {
        success: false,
        message: '退座请求出错'
      }
    }
  },

  // 获取当前活跃预约
  async fetchCurrentReservation({ commit, state, dispatch }) {
    try {
      // 获取当前用户ID
      const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
      const userId = currentUser.id

      console.log('获取当前预约 - 用户ID:', userId)

      // 从服务器获取最新状态
      const response = await getCurrentReservation(userId)
      console.log('从服务器获取的预约信息:', response)

      if (response.code === '0') {
        // 先检查是否有有效的数据对象
        if (response.data && typeof response.data === 'object' && !Array.isArray(response.data)) {
          // 打印详细的预约数据，便于调试
          console.log('服务器返回的预约数据详情:', JSON.stringify(response.data, null, 2))

          // 检查是否包含必要字段
          if (!response.data.reservationId && !response.data.id) {
            console.log('返回的预约数据缺少必要字段，视为无预约');
            commit('SET_ACTIVE_RESERVATION', null);
            localStorage.removeItem('activeReservation');
            return null;
          }

          // 处理后端返回的数据，确保包含必要的字段并统一状态表示
          const reservation = {
            ...response.data,
            // 如果后端返回的字段名称与前端不一致，进行转换
            reservationId: response.data.reservationId || response.data.id,
            floorNumber: response.data.floorNumber || response.data.floor_number || response.data.roomId || response.data.room_id,
            seatNumber: response.data.seatNumber || response.data.seat_number || response.data.number,
            startTime: response.data.startTime || response.data.start_time,
            endTime: response.data.endTime || response.data.end_time,
          }

          // 统一处理状态字段，将其转换为数字格式
          if (typeof response.data.status === 'number') {
            reservation.status = response.data.status;
          } else if (typeof response.data.status === 'string' && !isNaN(parseInt(response.data.status))) {
            reservation.status = parseInt(response.data.status);
          } else if (typeof response.data.status === 'string') {
            // 字符串状态转换为数字
            switch(response.data.status.toLowerCase()) {
              case 'reserved': reservation.status = 1; break;
              case 'checked_in':
              case 'occupied': reservation.status = 2; break;
              case 'timeout': reservation.status = 3; break;
              case 'cancelled':
              case 'canceled': reservation.status = 4; break;
              case 'completed': reservation.status = 5; break;
              default: reservation.status = 0; // 默认为可预约状态
            }
          } else {
            // 如果没有状态值，根据上下文判断
            // 注意：如果是从后端获取的活跃预约，默认应该是"预约中"状态
            // 如果后端返回了数据但没有status字段，我们假设它是一个有效预约
            reservation.status = 1;
          }

          console.log('处理后的预约数据:', reservation);

          // 更新座位状态以匹配预约状态
          if (reservation.floorNumber && reservation.seatNumber) {
            // 确保获取该楼层的座位数据
            if (!state.floors[reservation.floorNumber]) {
              await dispatch('fetchFloorSeats', reservation.floorNumber);
            }

            // 更新座位状态
            if (state.floors[reservation.floorNumber]) {
              commit('UPDATE_SEAT', {
                floorNumber: reservation.floorNumber,
                seatNumber: reservation.seatNumber,
                updates: { status: reservation.status }
              });
              console.log(`已同步座位状态为: ${reservation.status}`);
            }
          }

          commit('SET_ACTIVE_RESERVATION', reservation);
          return reservation;
        } else {
          // 服务器返回无效数据或字符串消息
          console.log('服务器未返回有效的预约数据:', response.data);
          commit('SET_ACTIVE_RESERVATION', null);
          localStorage.removeItem('activeReservation');
        }
      }

      return null;
    } catch (error) {
      console.error('获取当前预约失败:', error);
      commit('SET_ACTIVE_RESERVATION', null);
      localStorage.removeItem('activeReservation');
      return null;
    }
  },

  // 启动定时检查超时
  startTimeoutChecker({ commit, state, dispatch }) {
    // 先清除已有的检查器
    if (state.timeoutChecker) {
      clearInterval(state.timeoutChecker)
    }

    // 设置新的检查器，每分钟检查一次
    const checker = setInterval(async () => {
      if (state.activeReservation && state.activeReservation.reservationId) {
        try {
          const currentTime = new Date();
          const reservation = state.activeReservation;

          // 检查是否为"已预约"状态（未签到）
          const isReserved = reservation.status === 1 || reservation.status === '1';

          // 获取超时时间，可能是timeout_at字段或根据startTime计算
          let timeoutAt = null;
          if (reservation.timeout_at) {
            timeoutAt = new Date(reservation.timeout_at);
          } else if (reservation.startTime) {
            // 如果没有timeout_at字段，默认使用开始时间后30分钟作为超时时间
            const startTime = new Date(reservation.startTime);
            timeoutAt = new Date(startTime.getTime() + 30 * 60 * 1000);
          }

          console.log('超时检查:', {
            currentTime,
            timeoutAt,
            isReserved,
            reservation
          });

          // 如果处于预约状态且已超过最晚签到时间，则自动取消预约
          if (isReserved && timeoutAt && currentTime > timeoutAt) {
            console.log('预约已超时，自动取消:', reservation.reservationId);

            // 调用接口取消超时预约
            const response = await cancelReservation(reservation.reservationId, 'timeout');
            if (response.code === '0') {
              // 添加到历史预约
              commit('ADD_PAST_RESERVATION', {
                ...state.activeReservation,
                status: 3 // 使用数字3表示超时状态
              });

              // 清除活跃预约
              commit('SET_ACTIVE_RESERVATION', null);

              // 更新座位状态
              await dispatch('fetchSeatDetail', {
                floorNumber: reservation.floorNumber,
                seatNumber: reservation.seatNumber
              });

              // 直接更新座位状态为可预约
              if (state.floors[reservation.floorNumber]) {
                commit('UPDATE_SEAT', {
                  floorNumber: reservation.floorNumber,
                  seatNumber: reservation.seatNumber,
                  updates: { status: 0 } // 使用状态值0表示"可预约"
                });
              }

              // 在前端弹出通知
              Message.warning('您的预约已超时自动取消');
            } else {
              console.error('自动取消预约失败:', response);
            }
          } else {
            // 调用后端检查接口
            const response = await checkTimeout(state.activeReservation.reservationId);
            if (response.code === '0' && response.data && response.data.isTimeout) {
              console.log('后端返回超时状态，更新预约:', response.data);

              // 根据后端返回更新预约状态
              commit('ADD_PAST_RESERVATION', {
                ...state.activeReservation,
                status: 3 // 使用数字3表示超时状态
              });
              commit('SET_ACTIVE_RESERVATION', null);

              // 更新座位状态
              await dispatch('fetchSeatDetail', {
                floorNumber: state.activeReservation.floorNumber,
                seatNumber: state.activeReservation.seatNumber
              });

              // 直接更新座位状态为可预约
              if (state.floors[state.activeReservation.floorNumber]) {
                commit('UPDATE_SEAT', {
                  floorNumber: state.activeReservation.floorNumber,
                  seatNumber: state.activeReservation.seatNumber,
                  updates: { status: 0 } // 使用状态值0表示"可预约"
                });
              }
            }
          }
        } catch (error) {
          console.error('检查超时失败:', error);
        }
      }
    }, 60000); // 每分钟检查一次

    commit('SET_TIMEOUT_CHECKER', checker);
  },

  // 停止超时检查
  stopTimeoutChecker({ commit }) {
    commit('CLEAR_TIMEOUT_CHECKER')
  }
}

const getters = {
  currentFloorSeats: state => {
    return state.floors[state.currentFloor] || []
  },

  // 判断用户是否有活跃预约
  hasActiveReservation: state => {
    return !!state.activeReservation
  },

  // 获取用户当前占用的座位
  occupiedSeat: state => {
    if (!state.activeReservation) return null

    const { floorNumber, seatNumber } = state.activeReservation

    if (!state.floors[floorNumber]) return null

    return state.floors[floorNumber].find(
      seat => seat.seatNumber === seatNumber &&
             seat.status === SEAT_STATUS.OCCUPIED
    )
  },

  // 获取用户当前预约的座位
  reservedSeat: state => {
    if (!state.activeReservation) return null

    const { floorNumber, seatNumber } = state.activeReservation

    if (!state.floors[floorNumber]) return null

    return state.floors[floorNumber].find(
      seat => seat.seatNumber === seatNumber &&
             seat.status === SEAT_STATUS.RESERVED
    )
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}