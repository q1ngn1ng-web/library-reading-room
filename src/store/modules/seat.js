import { getSeatStatus, getFloors, getSeatDetail, reserveSeat, cancelReservation,
  checkIn, checkOut, getCurrentReservation, checkTimeout } from '@/api/seats'
import { SEAT_STATUS } from '@/utils/seatUtils'

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
      console.log('获取座位状态:', response.data)
      if (response.code === '0') {
        commit('SET_FLOOR_SEATS', {
          floorNumber,
          seats: response.data
        })
        return response.data
      }
      return []
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
  async reserveSeat({ commit, dispatch }, reservationData) {
    try {
      const response = await reserveSeat(reservationData)
      if (response.code === '0') {
        // 更新活跃预约
        commit('SET_ACTIVE_RESERVATION', {
          ...response.data,
          // 确保包含必要信息
          floorNumber: reservationData.floorNumber,
          seatNumber: reservationData.seatNumber
        })

        // 更新该座位状态
        await dispatch('fetchSeatDetail', {
          floorNumber: reservationData.floorNumber,
          seatNumber: reservationData.seatNumber
        })

        return {
          success: true,
          data: response.data
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
        // 添加到历史预约
        commit('ADD_PAST_RESERVATION', {
          ...state.activeReservation,
          status: 'canceled'
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
        // 更新活跃预约状态
        commit('SET_ACTIVE_RESERVATION', {
          ...state.activeReservation,
          status: 'checked_in',
          realStartTime: new Date().toISOString()
        })

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
        // 添加到历史预约
        commit('ADD_PAST_RESERVATION', {
          ...state.activeReservation,
          status: 'completed',
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
  async fetchCurrentReservation({ commit }) {
    try {
      // 首先检查本地存储
      const storedReservation = localStorage.getItem('activeReservation')
      if (storedReservation) {
        commit('SET_ACTIVE_RESERVATION', JSON.parse(storedReservation))
      }

      // 然后从服务器获取最新状态
      const response = await getCurrentReservation()
      if (response.code === '0' && response.data) {
        commit('SET_ACTIVE_RESERVATION', response.data)
        return response.data
      } else if (response.code === '0') {
        // 服务器返回空，说明没有活跃预约
        commit('SET_ACTIVE_RESERVATION', null)
      }

      return null
    } catch (error) {
      console.error('获取当前预约失败:', error)
      return null
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
          const response = await checkTimeout(state.activeReservation.reservationId)
          if (response.code === '0' && response.data.isTimeout) {
            // 如果超时，更新预约状态
            commit('ADD_PAST_RESERVATION', {
              ...state.activeReservation,
              status: 'timeout'
            })
            commit('SET_ACTIVE_RESERVATION', null)

            // 通知用户
            // 此处可以添加通知逻辑

            // 更新座位状态
            await dispatch('fetchSeatDetail', {
              floorNumber: state.activeReservation.floorNumber,
              seatNumber: state.activeReservation.seatNumber
            })
          }
        } catch (error) {
          console.error('检查超时失败:', error)
        }
      }
    }, 60000) // 每分钟检查一次

    commit('SET_TIMEOUT_CHECKER', checker)
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