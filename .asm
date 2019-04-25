  0         CALL         L10
  1         HALT   (0)   
  2  L10:   LOADL        0
  3  L11:   LOAD         3[LB]
  4         LOADL        5
  5         CALL         lt      
  6         JUMPIF (0)   L12
  7         LOAD         3[LB]
  8         LOADL        1
  9         CALL         add     
 10         STORE        3[LB]
 11         LOAD         3[LB]
 12         CALL         putintnl
 13         POP          0
 14         LOAD         3[LB]
 15         LOADL        1
 16         CALL         add     
 17         STORE        3[LB]
 18         JUMP         L11
 19  L12:   RETURN (0)   1
