function testDescribeMethods(obj)

am = getMethods(obj.server);
    
for i=1:length(am)
    meth = am(i);
   
    describeMethod(obj.server, meth.name);
        
end