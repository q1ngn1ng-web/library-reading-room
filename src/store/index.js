import Vue from 'vue'
import Vuex from 'vuex'
import chatModule from './modules/chat'
import seatModule from './modules/seat'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    chat: chatModule,
    seat: seatModule
  }
})