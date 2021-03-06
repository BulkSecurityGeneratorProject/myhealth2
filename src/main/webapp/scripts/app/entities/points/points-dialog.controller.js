'use strict';

angular.module('myhealthApp').controller('PointsDialogController',
    ['$scope', '$stateParams', '$uibModalInstance', 'entity', 'Points', 'User',
        function($scope, $stateParams, $uibModalInstance, entity, Points, User) {

            if (!entity.id) {
                entity.date = new Date();
                entity.exercise = 1;
                entity.meals = 1;
                entity.alcohol = 1;
            }
            
        $scope.points = entity;
        $scope.users = User.query();
        $scope.load = function(id) {
            Points.get({id : id}, function(result) {
                $scope.points = result;
            });
        };

        var onSaveSuccess = function (result) {
            $scope.$emit('myhealthApp:pointsUpdate', result);
            $uibModalInstance.close(result);
            $scope.isSaving = false;
        };

        var onSaveError = function (result) {
            $scope.isSaving = false;
        };

        $scope.save = function () {
            $scope.isSaving = true;
            if ($scope.points.id != null) {
                Points.update($scope.points, onSaveSuccess, onSaveError);
            } else {
                Points.save($scope.points, onSaveSuccess, onSaveError);
            }
        };

        $scope.clear = function() {
            $uibModalInstance.dismiss('cancel');
        };
}]);
