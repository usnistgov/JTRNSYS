function [ outputsToJava ] = JavaMatlabDemoFunction(inputsFromJava )
% =========================================================================
% Entry function for exchanging data between TRNSYS and MATLAB through the
% Java server
% =========================================================================
%% Return calculated values to simulate the 64-bit PVT example TRNSYS18
outputsToJava = PVTType277(inputsFromJava);

end

