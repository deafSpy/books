'use strict';

/**
 * Register controller.
 */
App.controller('Register', function($scope, $state, Restangular) {
    $scope.register = function () {
        console.log($scope.user)
        var promise = Restangular
            .one('user')
            .put($scope.user);
    
        promise.then(function () {
            $state.transitionTo('login');
        });
    }

    $scope.goToLogin = function () {
      $state.transitionTo('login'); 
    }
});