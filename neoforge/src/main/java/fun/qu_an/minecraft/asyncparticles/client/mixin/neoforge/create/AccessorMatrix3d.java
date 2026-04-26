package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.create;

import com.simibubi.create.foundation.collision.Matrix3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Matrix3d.class, remap = false)
public interface AccessorMatrix3d {
	@Accessor("m00")
	double m00();
	@Accessor("m01")
	double m01();
	@Accessor("m02")
	double m02();
	@Accessor("m10")
	double m10();
	@Accessor("m11")
	double m11();
	@Accessor("m12")
	double m12();
	@Accessor("m20")
	double m20();
	@Accessor("m21")
	double m21();
	@Accessor("m22")
	double m22();
	@Accessor("m00")
	void m00(double m00);
	@Accessor("m01")
	void m01(double m01);
	@Accessor("m02")
	void m02(double m02);
	@Accessor("m10")
	void m10(double m10);
	@Accessor("m11")
	void m11(double m11);
	@Accessor("m12")
	void m12(double m12);
	@Accessor("m20")
	void m20(double m20);
	@Accessor("m21")
	void m21(double m21);
	@Accessor("m22")
	void m22(double m22);
}
