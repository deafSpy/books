'use strict';

/**
 * User service.
 */
App.factory('User', function(Restangular) {
  var userInfo = null;
  
  return {
    /**
     * Returns user info.
     * @param force If true, force reloading data
     */
    userInfo: function(force) {
      if (userInfo == null || force) {
        userInfo = Restangular.one('user').get();
      }
      return userInfo;
      },
      
    
    /**
     * Registers an user.
     */
    //   register: function (user) {
    //   return Restangular.one('user').post('register', user);
    //   },
      
    /**
     * Login an user.
     */
      login: function (user) {
      return Restangular.one('user').post('login', user);
    },
    
    /**
     * Logout the current user.
     */
    logout: function() {
      return Restangular.one('user').post('logout', {});
    }
  }
});