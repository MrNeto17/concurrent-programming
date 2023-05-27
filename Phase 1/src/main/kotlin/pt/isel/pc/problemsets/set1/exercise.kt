import java.lang.Math.abs


    fun twoSum(nums: IntArray, target: Int): IntArray {
        val result = IntArray(2)
        for (i in 0 until nums.size){
            val missingIndex = nums.indexOf(abs(target-nums[i]))
            if (missingIndex != -1){
                result[0] = i
                result[1] = missingIndex
                return result
            }
        }
        return result
    }

fun main(){
    print(twoSum(intArrayOf(1,2), 3))
}