DECLARE 
  l_result CLOB := EMPTY_CLOB(); 
  l_line VARCHAR2(32767); 
  l_done INTEGER := 0; 
BEGIN 
  WHILE l_done != 1 LOOP 
    dbms_output.get_line(l_line, l_done); 
    l_result := l_result || l_line || CHR(10); 
  END LOOP; 
   
  :1 := l_result; 
   
END;