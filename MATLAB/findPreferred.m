function [result, findSeccess] = findPreferred(values, preferred, valuesName)
    result = values(find(strcmp(values, preferred), 1));
    findSeccess = true;
    if isempty(result)
        findSeccess = false;
        disp(['Can''t find preferred ' valuesName ': ' preferred]);
        if isempty(values)
            error(['There are no elements in ' valuesName]);
        else
            result = values(1);
        end
    end
    result = result{1};
    disp(['Select ' valuesName ': ' result]);
end
