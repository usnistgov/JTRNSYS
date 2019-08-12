function [ output_args ] = PVTType277( input_args )
% =========================================================================
% The PVT - example in 64-Bit TRNSYS18
% Inputs:
%       fLoad
%       
%  Outputs:
%       pLoad 
% ==========================================================================

fLoad = input_args(1);
pLoad = 500 * 3.6 * fLoad;

output_args = [pLoad];
end

